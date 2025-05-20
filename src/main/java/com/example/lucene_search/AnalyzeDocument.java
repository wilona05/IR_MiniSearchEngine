package com.example.lucene_search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.*;
import java.nio.file.*;

public class AnalyzeDocument {

    public static void main(String[] args) throws Exception {
        // ambil 1 dokumen untuk dicek
        Path path = Paths.get("docs/business/001.txt");
        // Path path = Paths.get("docs/entertainment/001.txt");
        // Path path = Paths.get("docs/tech/001.txt");
        
        BufferedReader reader = Files.newBufferedReader(path);
        
        // gabungan semua baris ke dalam satu string
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(" ");
        }

        String content = sb.toString();
        
        // pakai analyzer yang sama seperti saat indexing
        Analyzer analyzer = new StandardAnalyzer();

        TokenStream tokenStream = analyzer.tokenStream("contents", new StringReader(content));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        System.out.println("=== Hasil Preprocessing (Tokenisasi) ===");
        while (tokenStream.incrementToken()) {
            System.out.print(attr.toString() + " ");
        }
        tokenStream.end();
        tokenStream.close();
        analyzer.close();
    }
}
