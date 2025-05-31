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
            Directory indexDirectory = FSDirectory.open(Paths.get("docs/index"));    //bentuk folder index
            Analyzer analyzer = new StandardAnalyzer();                 //pembuatan variable untuk preprocessing
            IndexWriterConfig config = new IndexWriterConfig(analyzer); //konfig penulisan index
            IndexWriter writer = new IndexWriter(indexDirectory, config);   //membuat penulis yang terima dokumen dan simpan di index lucene

            File docsFolder = new File("docs");     //ambil dokumen
            File[] categoryDirs = docsFolder.listFiles(File::isDirectory);      //folder di dalam dokumen

            if (categoryDirs != null) {
                for (File categoryDir : categoryDirs) {
                    String category = categoryDir.getName(); // business, tech, entertainment
                    File[] files = categoryDir.listFiles((dir, name) -> name.endsWith(".txt"));     //ambil yang .txt

                    if (files != null) {
                        for (File file : files) {

                            // Baca semua baris
                            java.util.List<String> lines = Files.readAllLines(file.toPath());

                            if (lines.size() < 2) continue; // skip jika tidak valid karena format dokumen yang setiap paraf ada next line

                            String title = lines.get(0);    //dapatkan judul
                            String content = String.join(" ", lines.subList(1, lines.size()));  //baris pertama

                            Document doc = new Document();      //dokumen untuk lucene
                            //yang disimpan berupa judul dan konten
                            //beserta dengan nama file dan kategori(dari subfolder docs)
                            //.YES  menyimpan data asli di index
                            doc.add(new TextField("title", title, Field.Store.YES));
                            doc.add(new TextField("content", content, Field.Store.YES));
                            doc.add(new StringField("filename", file.getName(), Field.Store.YES));
                            doc.add(new StringField("category", category, Field.Store.YES));

                            //menyimpan term frequency dari isi content
                            FieldType fieldType = new FieldType(TextField.TYPE_STORED);
                            fieldType.setStoreTermVectors(true);
                            doc.add(new Field("tf", content, fieldType));

                            //masukan data ke docs lucene
                            writer.addDocument(doc);
                            System.out.println("Indexed: " + category + "/" + file.getName());
                        }
                    }
                }
            }

            writer.close();
            System.out.println("complete");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
