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
            String queryText = "gOoGle google lala";
            BytesRef term;
    
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
            int numDocs = reader.numDocs(); //banyak dokumen
            //System.out.println(numDocs);

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
            Map<String, Map<String, Double>> doc_term_tfidf = new HashMap<>();
            for(int i=0; i<numDocs; i++){
                Map<String, Double> all_term_tfidf = new HashMap<>(); //untuk menyimpan pasangan term dan tfidf (semua term yang ada di korpus)
                Map<String, Integer> tf_perDocs = new HashMap<>(); //untuk menyimpan pasangan term dan tf (semua term yang ada di dokumen ke-i)
                Terms docTerms = reader.getTermVector(i, fieldName); //ambil semua term pada dokumen ke-i
                
                //tf_perDocs: simpan seluruh term yang ada di dokumen ke-i, beserta tfnya
                if(docTerms != null){
                    TermsEnum termsEnum = docTerms.iterator(); //untuk iterasi tiap term
                    while((term = termsEnum.next()) != null){
                        String termText = termsEnum.term().utf8ToString(); //ubah term menjadi string
                        long tf = termsEnum.totalTermFreq(); //ambil tf dari termText
                        tf_perDocs.put(termText, (int)tf); //simpan pasangan term dan tf
                    }
                }

                //all_term_tf_idf: simpan seluruh term yang ada di korpus, beserta tf-idfnya
                for(Map.Entry<String, Double> entry : vocab_idf.entrySet()) { //iterasi seluruh key, value pada vocab_idf
                    String curTerm = entry.getKey();
                    Double curIdf = entry.getValue();
                    Integer tf = tf_perDocs.get(curTerm); //ambil tf curTerm di dokumen ke-i, null jika curTerm tidak ada di dokumen ke-i

                    //sublinear tf, weighted_tf=0 jika tf=0 atau tf=null, selain itu weighted_tf=1+log(tf)
                    double weighted_tf;
                    if(tf == null || tf <= 0){ 
                        weighted_tf = 0;
                    }else{
                        weighted_tf = 1 + Math.log((double)tf); 
                    }

                    double tfidf = weighted_tf * curIdf; //hitung tf-idf
                    all_term_tfidf.put(curTerm, tfidf); //simpan pasangan term dan tfidf
                    
                }

                all_term_tfidf = normalisasi(all_term_tfidf); //normalisasi
                String docName = reader.document(i).get("filename"); //nama dokumen
                doc_term_tfidf.put(docName, all_term_tfidf); //simpan seluruh pasangan term dan tfidf pada key docName
            }
            
            System.out.println(doc_term_tfidf.get(reader.document(0).get("filename")).size());
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
