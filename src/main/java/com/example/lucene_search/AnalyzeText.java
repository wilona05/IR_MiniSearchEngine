package com.example.lucene_search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;

public class AnalyzeText {

    public static void main(String[] args) throws IOException {
        // Contoh teks yang akan dianalisis
        String text = "Gallery unveils interactive tree";

        // Menggunakan StandardAnalyzer dari Lucene
        Analyzer analyzer = new StandardAnalyzer();

        // Proses analisis teks
        analyzeText(analyzer, text);

        // Jangan lupa close analyzer
        analyzer.close();
    }

    public static void analyzeText(Analyzer analyzer, String text) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(text));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        System.out.println("Token hasil preprocessing:");
        while (tokenStream.incrementToken()) {
            System.out.println("- " + attr.toString());
        }
        tokenStream.end();
        tokenStream.close();
    }
}
