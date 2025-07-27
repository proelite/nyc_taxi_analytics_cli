package com.xiaodi.taxi.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;

public class ParquetsDownloader {

    private static final String[] PARQUET_URLS = {
            "https://d37ci6vzurychx.cloudfront.net/trip-data/yellow_tripdata_2025-06.parquet",
            "https://d37ci6vzurychx.cloudfront.net/trip-data/yellow_tripdata_2025-05.parquet",
            "https://d37ci6vzurychx.cloudfront.net/trip-data/green_tripdata_2025-06.parquet",
            "https://d37ci6vzurychx.cloudfront.net/trip-data/green_tripdata_2025-05.parquet"
    };

    private static final Path OUTPUT_DIR = Paths.get("parquets");

    public static void main(String[] args) {
        try {
            // 1. Make sure the output directory exists
            if (Files.notExists(OUTPUT_DIR)) {
                Files.createDirectories(OUTPUT_DIR);
            }

            // 2. Download each file
            for (String urlString : PARQUET_URLS) {
                downloadParquet(urlString);
            }

            System.out.println("All files downloaded to " + OUTPUT_DIR.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("I/O error while creating output directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void downloadParquet(String urlString) {
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL: " + urlString);
            return;
        }

        String fileName = Paths.get(uri.getPath()).getFileName().toString();
        Path target = OUTPUT_DIR.resolve(fileName);

        System.out.println("Downloading " + fileName + " ...");

        try (InputStream in = uri.toURL().openStream()) {
            // Copy the stream to the target, replacing any existing file
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println(" → Saved as " + target);
        } catch (IOException e) {
            System.err.println("Failed to download " + urlString + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
