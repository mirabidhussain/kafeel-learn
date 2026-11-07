package com.mirabid.expenses;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Hands the exported Excel file to Gmail / Drive / WhatsApp. Only serves files from cache/exports. */
public class ExportProvider extends ContentProvider {
    static final String AUTHORITY = "com.mirabid.expenses.files";
    static final String MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private File fileFor(Uri uri) throws FileNotFoundException {
        String name = new File(uri.getPath() == null ? "" : uri.getPath()).getName();   // strips any ../
        File f = new File(new File(getContext().getCacheDir(), "exports"), name);
        if (!f.isFile()) throw new FileNotFoundException(name);
        return f;
    }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return MIME; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri, String[] projection, String sel, String[] args, String sort) {
        try {
            File f = fileFor(uri);
            MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
            c.addRow(new Object[]{f.getName(), f.length()});
            return c;
        } catch (FileNotFoundException e) { return null; }
    }

    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
