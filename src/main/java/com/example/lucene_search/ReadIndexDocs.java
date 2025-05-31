package com.example.lucene_search;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.nio.file.Paths;
import java.util.List;
import org.apache.lucene.index.IndexableField;

public class ReadIndexDocs {
    //untuk cek 

    public static void main(String[] args) throws Exception {
        String indexPath = "docs/index";

        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        IndexSearcher searcher = new IndexSearcher(reader);

        Query query = new MatchAllDocsQuery();
        TopDocs topDocs = searcher.search(query, 100);

        System.out.println("Total Dokumen Terindeks: " + topDocs.totalHits.value);

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.doc(scoreDoc.doc);

            String title = doc.get("title");
            String content = doc.get("content");
            String filename = doc.get("filename");
            String category = doc.get("category");

            System.out.println("\n--- Dokumen ---");
            System.out.println("Filename: " + (filename != null ? filename : "(tidak ada)"));
            System.out.println("Kategori: " + (category != null ? category : "(tidak ada)"));
            System.out.println("Judul: " + (title != null ? title : "(tidak ada)"));

            if (content != null && !content.isEmpty()) {
                System.out.println("Isi (ringkas): " +
                        (content.length() > 100 ? content.substring(0, 100) + "..." : content));    //bagian depan dari dokumen
            } else {
                //jika kosong
                System.out.println("Isi: (tidak ada field 'content')");
            }

            // (Opsional) Daftar field
            // System.out.println("Field-field lain:");
            // List<IndexableField> fields = doc.getFields();
            // for (IndexableField field : fields) {
            //     System.out.println(" - " + field.name());
            // }

            // int docID = scoreDoc.doc; 
            // Terms terms = reader.getTermVector(docID, "tf"); //ambil term vector dari field "tf"

            // if(terms != null){
            //     TermsEnum termsEnum = terms.iterator(); //untuk iterasi tiap term
            //     BytesRef term;

            //     System.out.println("=== Term Vector untuk dokumen " + docID + " ===");
            //     while ((term = termsEnum.next()) != null) {
            //         String termText = term.utf8ToString(); //ubah term ke string
            //         long freq = termsEnum.totalTermFreq(); // frekuensi term dalam dokumen
            //         System.out.println(termText + " (freq: " + freq + ")");
            //     }
            // }else {
            //     System.out.println("Tidak ada term vector yang disimpan dalam dokumen ini.");
            // }
        }

        reader.close();
    }
}
