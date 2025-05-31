package com.example.lucene_search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.*;

public class Vsm {
    public static void main(String[] args) {
        try{
            String indexPath = "docs/index";
            String fieldName = "tf";
            BytesRef term;
            //input query
            Scanner sc = new Scanner(System.in);
            System.out.print("Masukan query pencarian: ");
            String queryText = sc.nextLine();
    
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            int numDocs = reader.numDocs(); //banyak dokumen
            //System.out.println(numDocs);

            //PEMROSESAN DOKUMEN-----------------------------------------------------
            //menyimpan term seluruh dokumen beserta idfnya
            Map<String, Double> vocab_idf = new HashMap<>();
            for(int i=0; i<numDocs; i++){
                Terms terms = reader.getTermVector(i, fieldName); //ambil seluruh term vektor pada dokumen ke-i
                if(terms != null){
                    TermsEnum termsEnum = terms.iterator(); //untuk iterasi tiap term
                    while((term = termsEnum.next()) != null){
                        String termText = termsEnum.term().utf8ToString(); //ubah term menjadi string
                        if(vocab_idf.containsKey(termText) == false){ //term belum ada di vocab_idf
                            Term newTerm = new Term(fieldName, termText);
                            double df = reader.docFreq(newTerm); //hitung df untuk term
                            double idf = Math.log(numDocs/df); //hitung idf
                            // System.out.println(df + " " + idf);
                            vocab_idf.put(termText, idf); //simpan term dan idf
                        }
                    }
                }
            }
            // System.out.println(vocab_idf.size());

            //menyimpan dokumen, term yang muncul di dokumen tersebut, beserta tf-idfnya
            Map<Integer, Map<String, Double>> doc_term_tfidf = new HashMap<>();
            for(int i=0; i<numDocs; i++){
                Map<String, Double> tfidf_perDocs = new HashMap<>(); //untuk menyimpan pasangan term dan tf-idf (semua term yang ada di dokumen ke-i)
                Terms docTerms = reader.getTermVector(i, fieldName); //ambil semua term pada dokumen ke-i
                
                //tf_perDocs: simpan seluruh term yang ada di dokumen ke-i, beserta tfnya
                if(docTerms != null){
                    TermsEnum termsEnum = docTerms.iterator(); //untuk iterasi tiap term
                    while((term = termsEnum.next()) != null){
                        String termText = termsEnum.term().utf8ToString(); //ubah term menjadi string
                        Double idfTermText = vocab_idf.get(termText); //idf dari termText
                        long tf = termsEnum.totalTermFreq(); //ambil tf dari termText
                        //sublinear tf, weighted_tf=0 jika tf=0, selain itu weighted_tf=1+log(tf)
                        double weighted_tf = 0;
                        if(tf > 0){ 
                            weighted_tf = 1 + Math.log((double)tf); 
                        }

                        double tfidf = weighted_tf * idfTermText; //hitung tf-idf
                        tfidf_perDocs.put(termText, tfidf); //simpan pasangan term dan tf-idf
                    }
                }

                tfidf_perDocs = normalisasi(tfidf_perDocs); //normalisasi
                doc_term_tfidf.put(i, tfidf_perDocs); //simpan seluruh pasangan term dan tfidf pada key index dokumen
            }
            // System.out.println(doc_term_tfidf.get(reader.document(0).get("filename")).size());

            //PEMROSESAN QUERY-----------------------------------------------------
            Map<String, Integer> query_term_tf = new HashMap<>(); //untuk menyimpan pasangan term dan term frequency
            //preprocessing queryText
            StandardAnalyzer analyzer = new StandardAnalyzer();
            var tokenStream = analyzer.tokenStream(fieldName, new StringReader(queryText));
            var attr = tokenStream.addAttribute(org.apache.lucene.analysis.tokenattributes.CharTermAttribute.class);
            tokenStream.reset();
            while(tokenStream.incrementToken()){ //iterasi tiap term pada queryText
                String curTerm = attr.toString();
                // simpan term dan hitung tf-df
                // jika term belum ada, defaultValue (0) + 1
                // jika term sudah ada, ambil valuenya kemudian tambah 1
                query_term_tf.put(curTerm, query_term_tf.getOrDefault(curTerm, 0)+1); 
                // System.out.println(curTerm+" "+query_term_tf.get(curTerm));
            }
            tokenStream.end();
            tokenStream.close();
            analyzer.close();

            //menyimpan term yang muncul di query, beserta tf-idfnya (hanya untuk term yang muncul di korpus)
            Map<String, Double> tfidf_query = new HashMap<>(); 
            for(Map.Entry<String, Integer> entry : query_term_tf.entrySet()){ 
                String curTerm =entry.getKey(); //term
                Double idfTermText = vocab_idf.get(curTerm); //idf dari curTerm
                if(idfTermText != null){ //term ada di korpus
                    Integer tf = entry.getValue(); //nilai tf 
                    //sublinear tf, weighted_tf=0 jika tf=0, selain itu weighted_tf=1+log(tf)
                    double weighted_tf = 0;
                    if(tf > 0){ 
                        weighted_tf = 1 + Math.log((double)tf); 
                    }
    
                    double tfidf = weighted_tf * idfTermText; //hitung tf-idf
                    tfidf_query.put(curTerm, tfidf); //simpan pasangan term dan tf-idf
                    // System.out.println(curTerm);
                    // System.out.println(vocab_idf.get(curTerm));
                    // System.out.println(weighted_tf);
                    // System.out.println(tfidf_query.get(curTerm));
                }
            }
            tfidf_query = normalisasi(tfidf_query); //normalisasi

            //PEMERINGKATAN DOKUMEN (COSINE SIMILARITY)-----------------------------------------------------
            Map<Integer, Double> docScores = new HashMap<>(); //untuk menyimpan skor dokumen
            for(int i=0; i<numDocs; i++){ //iterasi seluruh dokumen
                Map<String, Double> tfidf_curDoc = doc_term_tfidf.get(i); //ambil seluruh tf-idf dokumen ke-i
                double score = 0.0;
                for(Map.Entry<String, Double> entry : tfidf_query.entrySet()){ //iterasi tiap term yang ada di query
                    String curTerm = entry.getKey(); //term
                    Double tfidf_q = entry.getValue(); //tf-idf query
                    Double tfidf_d = tfidf_curDoc.get(curTerm); //ambil tf-idf curTerm pada dokumen ke-i
                    if(tfidf_d != null){ //jika curTerm ada di dokumen ke-i, tambahkan skornya
                        score += tfidf_q*tfidf_d; 
                    }
                }
                docScores.put(i, score); //simpan index dokumen dan skornya
            }

            //urutkan skor dari yang tertinggi
            List<Map.Entry<Integer, Double>> sortedDocs = new ArrayList<>(docScores.entrySet());
            sortedDocs.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); 
            // System.out.println(sortedDocs);

            //HASIL-----------------------------------------------------
            System.out.println("Query: " + queryText);
            System.out.println("Dokumen:");
            for(int i=0; i<sortedDocs.size(); i++){
                Integer idxDoc = sortedDocs.get(i).getKey(); //index dokumen
                Double score = sortedDocs.get(i).getValue(); //skor dokumen
                String title = reader.document(idxDoc).get("title"); //judul dokumen
                String fileName = reader.document(idxDoc).get("filename"); //nama dokumen
                String category = reader.document(idxDoc).get("category"); //kategori dokumen
                String content = reader.document(idxDoc).get("content"); //kategori dokumen
                System.out.printf("%d skor: %.4f | judul: $s | dokumen: %s | kategori: %s\n", i+1, score, title, fileName, category);
                System.out.println("Isi (ringkas): " +
                        (content.length() > 100 ? content.substring(0, 100) + "..." : content));
                System.out.println("--------------------------------------");
            }
            
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    //fungsi untuk normalisasi
    private static Map<String, Double> normalisasi(Map<String, Double> doc){
        Double norm = 0.0; //panjang vektor
        for(Map.Entry<String, Double> entry : doc.entrySet()){
            Double tfidf = entry.getValue(); //nilai tf idf
            norm += tfidf * tfidf; //hitung panjang vektor doc untuk normalisasi
        }
        norm = Math.sqrt(norm);

        Map<String, Double> normalized_doc = new HashMap<>(); //untuk menyimpan hasil normalisasi
        for(Map.Entry<String, Double> entry : doc.entrySet()){
            String term = entry.getKey(); //term
            Double tfidf = entry.getValue(); //nilai tf idf
            normalized_doc.put(term, tfidf/norm); //simpan hasil normalisasi
        }
        return normalized_doc;
    }
}
