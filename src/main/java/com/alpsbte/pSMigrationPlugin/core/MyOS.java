package com.alpsbte.pSMigrationPlugin.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

public class MyOS extends GZIPOutputStream {
    public MyOS(OutputStream out) throws IOException {
        super(out);
        this.def.end();
        this.def = new Deflater(Deflater.NO_COMPRESSION);
    }
}