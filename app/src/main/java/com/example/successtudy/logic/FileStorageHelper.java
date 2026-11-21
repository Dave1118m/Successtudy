package com.example.successtudy.logic;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileStorageHelper {
    private static final String TAG = "FileStorageHelper";
    private final Context context;

    public FileStorageHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Extracts full text from PDF, TXT, DOCX, and PPTX files.
     */
    public String extractTextFromFile(String absolutePath) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) return "";
        String lower = absolutePath.toLowerCase();

        try {
            if (lower.endsWith(".pdf")) {
                // PDF extraction via PDFBox Android
                PDFBoxResourceLoader.init(context);
                File pdfFile = new File(absolutePath);
                if (!pdfFile.exists()) return "";
                try (PDDocument document = PDDocument.load(pdfFile)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    int totalPages = document.getNumberOfPages();
                    stripper.setStartPage(1);
                    stripper.setEndPage(Math.min(totalPages, 20));
                    String parsedText = stripper.getText(document);
                    return parsedText != null ? parsedText.trim() : "";
                }
            } else if (lower.endsWith(".txt")) {
                StringBuilder out = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(absolutePath), "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        out.append(line).append('\n');
                    }
                }
                return out.toString().trim();
            } else if (lower.endsWith(".docx")) {
                try (ZipFile zip = new ZipFile(absolutePath)) {
                    ZipEntry entry = zip.getEntry("word/document.xml");
                    if (entry == null) return "";
                    try (InputStream in = zip.getInputStream(entry);
                         BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                        StringBuilder xml = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) xml.append(line);
                        return xml.toString().replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
                    }
                }
            } else if (lower.endsWith(".pptx")) {
                StringBuilder sb = new StringBuilder();
                try (ZipFile zip = new ZipFile(absolutePath)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry e = entries.nextElement();
                        String name = e.getName();
                        if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                            try (InputStream in = zip.getInputStream(e);
                                 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                                String line;
                                while ((line = br.readLine()) != null) sb.append(line).append(' ');
                            }
                        }
                    }
                }
                return sb.toString().replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting text from " + absolutePath, e);
        }
        return "";
    }

    /**
     * Copies the content from the given {@link Uri} to external public documents directory.
     */
    public String copyToExternalPublic(Uri uri, String fileName) {
        try {
            File docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!docsDir.exists()) {
                docsDir.mkdirs();
            }
            File destFile = new File(docsDir, fileName);

            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
                if (in == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Error copying file to public documents", e);
            return null;
        }
    }
}
