package scouter.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * http://nowonbun.tistory.com/320 [명월 일지]
 */
public class ZipUtil {
    /**
     * 압축 메소드
     *
     * @param path           경로
     * @param outputFileName 출력파일명
     */
    public static void compress(String path, String outputFileName) throws Throwable {
        File file = new File(path);
        int pos = outputFileName.lastIndexOf(".");
        if (!outputFileName.substring(pos).equalsIgnoreCase(".zip")) {
            outputFileName += ".zip";
        }
        // 압축 경로 체크
        if (!file.exists()) {
            throw new Exception("Not File!");
        }
        // 출력 스트림
        FileOutputStream fos = null;
        // 압축 스트림
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(new File(outputFileName));
            zos = new ZipOutputStream(fos);
            // 디렉토리 검색
            searchDirectory(file, zos);
        } catch (Throwable e) {
            throw e;
        } finally {
            if (zos != null) zos.close();
            if (fos != null) fos.close();
        }
    }

    /**
     * 다형성
     */
    private static void searchDirectory(File file, ZipOutputStream zos) throws Throwable {
        searchDirectory(file, file.getPath(), zos);
    }

    /**
     * 디렉토리 탐색
     *
     * @param file 현재 파일
     * @param root 루트 경로
     * @param zos  압축 스트림
     */
    private static void searchDirectory(File file, String root, ZipOutputStream zos)
            throws Exception {
        //지정된 파일이 디렉토리인지 파일인지 검색
        if (file.isDirectory()) {
            //디렉토리일 경우 재탐색(재귀)
            File[] files = file.listFiles();
            for (File f : files) {
                searchDirectory(f, root, zos);
            }
        } else {
            //파일일 경우 압축을 한다.
            compressZip(file, root, zos);
        }
    }

    /**
     * 압축 메소드
     *
     * @param file
     * @param root
     * @param zos
     * @throws Exception
     */
    private static void compressZip(File file, String root, ZipOutputStream zos) throws Exception {
        FileInputStream fis = null;
        try {
            String zipName = file.getPath().replace(root + "\\", "");
            zipName = zipName.replace(root + "/", "");

            System.out.println("zipname:" + zipName);
            // 파일을 읽어드림
            fis = new FileInputStream(file);
            // Zip엔트리 생성(한글 깨짐 버그)
            ZipEntry zipentry = new ZipEntry(zipName);
            // 스트림에 밀어넣기(자동 오픈)
            zos.putNextEntry(zipentry);
            int length = (int) file.length();
            byte[] buffer = new byte[length];
            //스트림 읽어드리기
            fis.read(buffer, 0, length);
            //스트림 작성
            zos.write(buffer, 0, length);
            //스트림 닫기
            zos.closeEntry();

        } catch (Throwable e) {
            throw e;
        } finally {
            if (fis != null) fis.close();
        }
    }

    /**
     * 압축풀기 메소드
     *
     * @param zipFileName 압축파일
     * @param directory   압축 풀 폴더
     */
    public static void decompress(String zipFileName, String directory) throws Throwable {
        File zipFile = new File(zipFileName);
        FileInputStream fis = null;
        ZipInputStream zis = null;
        ZipEntry zipentry = null;
        try {
            //파일 스트림
            fis = new FileInputStream(zipFile);
            //Zip 파일 스트림
            zis = new ZipInputStream(fis);
            //entry가 없을때까지 뽑기
            while ((zipentry = zis.getNextEntry()) != null) {
                String filename = zipentry.getName();
                File file = new File(directory, filename);
                //entiry가 폴더면 폴더 생성
                if (zipentry.isDirectory()) {
                    file.mkdirs();
                } else {
                    //파일이면 파일 만들기
                    createFile(file, zis);
                }
            }
        } catch (Throwable e) {
            throw e;
        } finally {
            if (zis != null)
                zis.close();
            if (fis != null)
                fis.close();
        }
    }

    /**
     * 파일 만들기 메소드
     *
     * @param file 파일
     * @param zis  Zip스트림
     */
    private static void createFile(File file, ZipInputStream zis) throws Throwable {
        //디렉토리 확인
        File parentDir = new File(file.getParent());
        //디렉토리가 없으면 생성하자
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        //파일 스트림 선언
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[256];
            int size = 0;
            //Zip스트림으로부터 byte뽑아내기
            while ((size = zis.read(buffer)) > 0) {
                //byte로 파일 만들기
                fos.write(buffer, 0, size);
            }
        } catch (Throwable e) {
            throw e;
        }
    }
}
