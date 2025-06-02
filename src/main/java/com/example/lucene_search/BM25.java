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

    static final float k1 = 1.5f;
    static final float b = 0.75f;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            //baca index lucene dari folder docs/index
            DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("docs/index")));
            IndexSearcher searcher = new IndexSearcher(reader);

            String field = "bm";
            System.out.print("Masukan query pencarian: ");
            String query = sc.nextLine();

            //tokenisasi query dengan pemisah spasi
            String[] terms = query.toLowerCase().split("\\s+");

            Set<Integer> validDocs = new HashSet<>(); //untuk menyimpan dokumen yang relevan
            Map<Integer, Integer> docLengths = new HashMap<>(); //menyimpan panjang dokumen
            long totalLength = 0; //untuk total panjang semua dokumen yang valid

            // loop untuk mencari dokumen yang berisi masing-masing kata dalam query
            for (String termText : terms) {
                Query queryLucene = new TermQuery(new Term(field, termText)); //membuat query
                TopDocs results = searcher.search(queryLucene, 50); //ambil 50 dokumen yang terbaik

                // loop untuk menyimpan dokumen yang ditemukan dan menghitung panjang dokumen
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    int docId = scoreDoc.doc; //ambil id dokumen
                    validDocs.add(docId); //simpan id dokumen

                    Terms termVector = reader.getTermVector(docId, field); //ambil term vektor dari dokumen
                    if (termVector != null) {
                        int docLength = (int) termVector.getSumTotalTermFreq(); //hitung panjang dokumen
                        docLengths.put(docId, docLength); //simpan panjang dokumen
                        totalLength += docLength; //tambahkan panjang dokumen ke total
                    }
                }
            }

            //itung panjang rata-rata dokumen 
            float avgdl = (float) totalLength / Math.max(1, validDocs.size());

            Map<Integer, Float> docScores = new HashMap<>(); //untuk menyimpan skor setiap dokumen

            // loop untuk menghitung skor BM25 untuk setiap term dalam query
            for (String termText : terms) {
                Term term = new Term(field, termText); //membuat term dari query
                long df = reader.docFreq(term); //itung jumlah dokumen yang mengandung term 
                if (df == 0) continue;

                float idf = (float) Math.log(1 + (reader.maxDoc() - df + 0.5) / (df + 0.5)); //itung nilai idf

                //loop untuk menghitung skor BM25 setiap dokumen dalam validDocs
                for (int docId : validDocs) {
                    Terms termsEnumSource = reader.getTermVector(docId, field); //ambil term vector dokumen
                    if (termsEnumSource != null) {
                        TermsEnum termsEnum = termsEnumSource.iterator(); //iterasi semua term dalam dokumen 
                        while (termsEnum.next() != null) { // iterasi semua terms diperiksa
                            String indexedTerm = termsEnum.term().utf8ToString(); //ambil term 
                            if (indexedTerm.equals(termText)) { //jika cocok dengan query
                                PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.FREQS);
                                while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) { //iterasi posting list 
                                    int matchedDocId = docId; //untuk pastikan dokumen yang digunakan benar
                                    int tf = postingsEnum.freq(); //ambil term frekuensi 
                                    int docLength = docLengths.getOrDefault(matchedDocId, 1); //ambil panjang dokumen 

                                    //itung skor BM25
                                    float score = idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * docLength / avgdl));

                                    //simpan skor dokumen
                                    docScores.put(matchedDocId, docScores.getOrDefault(matchedDocId, 0f) + score);
                                }
                            }
                        }
                    }
                }
            }

            //urutkan dokumen berdasarkan skor mulai dari tertinggi ke terendah 
            List<Map.Entry<Integer, Float>> sortedDocs = new ArrayList<>(docScores.entrySet());
            sortedDocs.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

            //tampilkan hasilnya
            System.out.println("BM25 Ranking : " + query);
            System.out.println("Total documents ditemukan: " + docScores.size());

            for (Map.Entry<Integer, Float> entry : sortedDocs) {
                Document doc = reader.document(entry.getKey());
                System.out.printf("Score: %.4f | Title: %s | File: %s | Category: %s\n",
                        entry.getValue(),
                        doc.get("title"),
                        doc.get("filename"),
                        doc.get("category"));
            }

            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
