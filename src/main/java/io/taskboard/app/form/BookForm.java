package io.taskboard.app.form;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by akiraabe on 2017/04/29.
 */
@Data
public class BookForm implements Serializable {

    private MultipartFile[] files;

    private MultipartFile file1;
    private MultipartFile file2;
    private MultipartFile file3;
    private MultipartFile file4;
    private MultipartFile file5;
    private MultipartFile file6;

    private String title;
    private String publisher;
    private String author;
    private Date publishDate;

    private String memo;
}
