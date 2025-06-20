package com.example.lucene_search;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

public class BM25 {

    static final float k = 1.5f;
    static final float b = 0.75f;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            // baca index lucene dari folder docs/index
            DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("docs/index")));

            String field = "bm";
            System.out.print("Masukan query pencarian: ");
            String query = sc.nextLine();

            // tokenisasi query dengan pemisah spasi
            String[] terms = query.toLowerCase().split("\\s+");

            Map<Integer, Integer> docLengths = new HashMap<>(); // menyimpan panjang dokumen
            long totalLength = 0; // total panjang semua dokumen
            int totalDocsInCorpus = reader.maxDoc(); // jumlah dokumen yang ada di corpus

            IndexSearcher searcher = new IndexSearcher(reader);

            // menghitung banyak dokumen yang relevan untuk mengetahui nilai R
            Set<Integer> validDocs = new HashSet<>();
            for (String termText : terms) {
                Query queryLucene = new TermQuery(new Term(field, termText));
                TopDocs results = searcher.search(queryLucene, 30);
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    validDocs.add(scoreDoc.doc); // Menyimpan docID yang relevan
                }
            }

            int R = validDocs.size();

            // hitung panjang seluruh dokumen di corpus
            for (int docId = 0; docId < totalDocsInCorpus; docId++) {
                Terms termVector = reader.getTermVector(docId, field);
                if (termVector != null) {
                    int docLength = (int) termVector.getSumTotalTermFreq();
                    docLengths.put(docId, docLength);
                    totalLength += docLength;
                }
            }

            // itung panjang rata-rata dokumen
            float avgdl = (float) totalLength / totalDocsInCorpus;

            Map<Integer, Float> docScores = new HashMap<>(); // untuk menyimpan skor setiap dokumen

            // loop untuk menghitung skor BM25 untuk setiap term dalam query
            for (String termText : terms) {
                Term term = new Term(field, termText); // membuat term dari query
                long df = reader.docFreq(term); // itung jumlah dokumen yang mengandung term
                if (df == 0)
                    continue;

                //float idf = (float) Math.log(1 + (reader.maxDoc() - df + 0.5) / (df + 0.5)); // itung nilai idf

                long Nt = df; // jumlah dokumen yang mengandung term
                long rt = 0; // jumlah dokumen relevan yang mengandung term

                //loop untung menghitung jumlah dokumen yang dianggap relevan dengan kueri
                for (int docId : validDocs) { // validDocs berisi dokumen relevan
                    Terms termVector = reader.getTermVector(docId, field);
                    if (termVector != null) {
                        TermsEnum termsEnum = termVector.iterator();
                        while (termsEnum.next() != null) {
                            if (termsEnum.term().utf8ToString().equals(termText)) {
                                rt++; // Menambah jumlah dokumen relevan yang mengandung term
                            }
                        }
                    }
                }

                //itung nilai wt
                float wt = (float) Math.log((rt + 0.5) * (totalDocsInCorpus - R + 1) / ((R + 1) * (Nt - rt + 0.5)));

                // loop untuk menghitung skor BM25 setiap dokumen dalam validDocs
                for (int docId : docLengths.keySet()) {
                    Terms termsEnumSource = reader.getTermVector(docId, field); // ambil term vector dokumen
                    if (termsEnumSource != null) {
                        TermsEnum termsEnum = termsEnumSource.iterator(); // iterasi semua term dalam dokumen
                        while (termsEnum.next() != null) { // iterasi semua terms diperiksa
                            String indexedTerm = termsEnum.term().utf8ToString(); // ambil term
                            if (indexedTerm.equals(termText)) { // jika cocok dengan query
                                PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
                                while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) { // iterasi posting list
                                    int matchedDocId = docId; // untuk pastikan dokumen yang digunakan benar
                                    int tf = postingsEnum.freq(); // ambil term frekuensi
                                    int docLength = docLengths.getOrDefault(matchedDocId, 1); // ambil panjang dokumen

                                    // itung skor BM25
                                    float score = wt * tf * (k + 1) / (tf + k * ((1 - b) + b * (docLength / avgdl)));

                                    // simpan skor dokumen
                                    docScores.put(matchedDocId, docScores.getOrDefault(matchedDocId, 0f) + score);
                                }
                            }
                        }
                    }
                }
            }

            // urutkan dokumen berdasarkan skor mulai dari tertinggi ke terendah
            List<Map.Entry<Integer, Float>> sortedDocs = new ArrayList<>(docScores.entrySet());
            sortedDocs.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

            // tampilkan hasilnya
            System.out.println("--------------------------------------\n");
            System.out.println("BM25 ranking untuk query: " + query);
            System.out.println("Total dokumen yang relevan: " + docScores.size());

            int rank = 1;
            for (Map.Entry<Integer, Float> entry : sortedDocs) {
                Document doc = reader.document(entry.getKey());

                String content = doc.get("content");
                String preview = (content != null && content.length() > 100) ? content.substring(0, 100) + "..."
                        : content;

                System.out.printf("%d. Score: %.4f | Judul: %s | Dokumen: %s | Kategori: %s\n",
                        rank++,
                        entry.getValue(),
                        doc.get("title"),
                        doc.get("filename"),
                        doc.get("category"));
                System.out.println("Isi (ringkas): " + (preview != null ? preview : "(Tidak ada konten)"));
                System.out.println("--------------------------------------");
            }

            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
