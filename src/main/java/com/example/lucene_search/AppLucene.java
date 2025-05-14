package com.example.lucene_search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.*;
import java.nio.file.*;

public class AppLucene {

    public static void main(String[] args) {
        try {
            Directory indexDirectory = FSDirectory.open(Paths.get("index"));
            //preprocessing
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(indexDirectory, config);

            File docsFolder = new File("docs");
            File[] categoryDirs = docsFolder.listFiles(File::isDirectory);

            if (categoryDirs != null) {
                for (File categoryDir : categoryDirs) {
                    String category = categoryDir.getName(); // business, tech, entertainment
                    File[] files = categoryDir.listFiles((dir, name) -> name.endsWith(".txt"));

                    if (files != null) {
                        for (File file : files) {
                            // Baca semua baris
                            java.util.List<String> lines = Files.readAllLines(file.toPath());

                            if (lines.size() < 2) continue; // Skip jika tidak valid

                            String title = lines.get(0);
                            String content = String.join(" ", lines.subList(1, lines.size()));

                            Document doc = new Document();
                            doc.add(new TextField("title", title, Field.Store.YES));
                            doc.add(new TextField("content", content, Field.Store.YES));
                            doc.add(new StringField("filename", file.getName(), Field.Store.YES));
                            doc.add(new StringField("category", category, Field.Store.YES));

                            writer.addDocument(doc);
                            System.out.println("Indexed: " + category + "/" + file.getName());
                        }
                    }
                }
            }

            writer.close();
            System.out.println("Complete indexing.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
