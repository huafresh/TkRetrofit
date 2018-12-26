package com.thinkive.android.network;

/**
 * @author hua
 * @version V1.0
 * @date 2018/12/25 16:44
 */

public class NoticeInfoBean {

    /**
     * summary :
     * id : 89d8845ee73301e6548288cc40c229a8_2
     * title : 关于对冯××采取限制开仓监管措施的公告
     * source : 上海期货交易所
     * infopubldate : 2018-12-24 00:00:00
     */

    private String summary;
    private String id;
    private String title;
    private String source;
    private String infopubldate;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getInfopubldate() {
        return infopubldate;
    }

    public void setInfopubldate(String infopubldate) {
        this.infopubldate = infopubldate;
    }
}
