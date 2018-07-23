package eu.area;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class UnZip{

    private File zipfile;
    private Updater frame;

    public UnZip(File zipfile, Updater frame){
        this.zipfile = zipfile;
        this.frame = frame;
    }

    public void start() throws Exception {
        frame.isWorking = true;
        FileInputStream is = new FileInputStream(zipfile.getCanonicalFile());
        FileChannel channel = is.getChannel();
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        ZipEntry ze = null;
        try {
            while ((ze = zis.getNextEntry()) != null) {
                File f = new File(ze.getName());
                if (ze.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                if (f.getParentFile() != null) {
                    f.getParentFile().mkdirs();
                }
                OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
                try {
                    try {
                        final byte[] buf = new byte[1024];
                        int bytesRead;
                        long nread = 0L;
                        long length = zipfile.length();

                        while (-1 != (bytesRead = zis.read(buf))){
                            fos.write(buf, 0, bytesRead);
                            nread += bytesRead;
                            frame.progressBar.setValue((int)((float)channel.position()/length * 100));
                        }
                    } finally {
                        fos.close();
                    }
                } catch (final IOException ioe) {
                    f.delete();
                    throw ioe;
                }
            }
        } finally {
            zis.close();
            finish();
        }
    }

    private void finish(){
        frame.isWorking = false;
    }

}