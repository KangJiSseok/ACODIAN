package com.ibank.axwms.domain.file;

public enum FileType {
    DOCX("docx"),
    HWP("hwp"),
    MD("md"),
    PDF("pdf"),
    PNG("png"),
    PPTX("pptx"),
    XLSX("xlsx");

    private final String dbExtension;

    FileType(String dbExtension) {
        this.dbExtension = dbExtension;
    }

    /**
     * 업로드 저장 정책과 같은 소문자 확장자 vocabulary 로 repository 조건 값을 제공한다.
     */
    public String dbExtension() {
        return dbExtension;
    }
}
