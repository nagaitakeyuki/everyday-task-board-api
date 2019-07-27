package io.taskboard.app.form;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * Created by nagai on 2019/06/18.
 */
@Data
public class DailyReportForm implements Serializable {

    private MultipartFile[] files;

    private String date;

    private String shopName;

    private String staffName;

    private Integer numOfCustomer;

    private Long sales;

    private Long unitPrice;

    private Long pace;

    private Long totalSales;

    private Long cash;

    private Long credit;

    private Long hpPoint;

    private Long employeeDiscount;

    private Long aCard;
}
