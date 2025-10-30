package mcheli;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeSet;

public class MCH_FileSearch {

   public static final int TYPE_FILE_OR_DIR = 1;
   public static final int TYPE_FILE = 2;
   public static final int TYPE_DIR = 3;
   private TreeSet set = new TreeSet();


   public File[] listFiles(String directoryPath, String fileName) {
      if(fileName != null) {
         fileName = fileName.replace(".", "\\.");
         fileName = fileName.replace("*", ".*");
      }

      return this.listFiles(directoryPath, fileName, 2, true, 0);
   }

   public File[] listFiles(String directoryPath, String fileNamePattern, int type, boolean isRecursive, int period) {
      File dir = new File(directoryPath);
      if(!dir.isDirectory()) {
         throw new IllegalArgumentException("The path specified by the argument [" + dir.getAbsolutePath() + "] is not a directory.");
      } else {
         File[] files = dir.listFiles();

         for(int i = 0; i < files.length; ++i) {
            File file = files[i];
            this.addFile(type, fileNamePattern, this.set, file, period);
            if(isRecursive && file.isDirectory()) {
               this.listFiles(file.getAbsolutePath(), fileNamePattern, type, isRecursive, period);
            }
         }

         return (File[])((File[])this.set.toArray(new File[this.set.size()]));
      }
   }

   private void addFile(int type, String match, TreeSet set, File file, int period) {
      switch(type) {
      case 2:
         if(!file.isFile()) {
            return;
         }
         break;
      case 3:
         if(!file.isDirectory()) {
            return;
         }
      }

      if(match == null || file.getName().matches(match)) {
         if(period != 0) {
            Date lastModifiedDate = new Date(file.lastModified());
            String lastModifiedDateStr = (new SimpleDateFormat("yyyyMMdd")).format(lastModifiedDate);
            long oneDayTime = 86400000L;
            long periodTime = oneDayTime * (long)Math.abs(period);
            Date designatedDate = new Date(System.currentTimeMillis() - periodTime);
            String designatedDateStr = (new SimpleDateFormat("yyyyMMdd")).format(designatedDate);
            if(period > 0) {
               if(lastModifiedDateStr.compareTo(designatedDateStr) < 0) {
                  return;
               }
            } else if(lastModifiedDateStr.compareTo(designatedDateStr) > 0) {
               return;
            }
         }

         set.add(file);
      }
   }

   public void clear() {
      this.set.clear();
   }
}
