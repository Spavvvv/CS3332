package src.model.classroom;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class for classroom data.
 */
public class Classroom {
    private final IntegerProperty id;
    private final IntegerProperty stt;
    private final StringProperty ma;
    private final StringProperty ten;
    private final IntegerProperty tang;
    private final IntegerProperty sucChua;
    private final StringProperty trangThai;

    public Classroom(int id, int stt, String ma, String ten, int tang, int sucChua, String trangThai) {
        this.id = new SimpleIntegerProperty(id);
        this.stt = new SimpleIntegerProperty(stt);
        this.ma = new SimpleStringProperty(ma);
        this.ten = new SimpleStringProperty(ten);
        this.tang = new SimpleIntegerProperty(tang);
        this.sucChua = new SimpleIntegerProperty(sucChua);
        this.trangThai = new SimpleStringProperty(trangThai);
    }

    // Getters
    public int getId() { return id.get(); }
    public int getStt() { return stt.get(); }
    public String getMa() { return ma.get(); }
    public String getTen() { return ten.get(); }
    public int getTang() { return tang.get(); }
    public int getSucChua() { return sucChua.get(); }
    public String getTrangThai() { return trangThai.get(); }

    // Property getters for JavaFX binding
    public IntegerProperty idProperty() { return id; }
    public IntegerProperty sttProperty() { return stt; }
    public StringProperty maProperty() { return ma; }
    public StringProperty tenProperty() { return ten; }
    public IntegerProperty tangProperty() { return tang; }
    public IntegerProperty sucChuaProperty() { return sucChua; }
    public StringProperty trangThaiProperty() { return trangThai; }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setStt(int stt) { this.stt.set(stt); }
    public void setMa(String ma) { this.ma.set(ma); }
    public void setTen(String ten) { this.ten.set(ten); }
    public void setTang(int tang) { this.tang.set(tang); }
    public void setSucChua(int sucChua) { this.sucChua.set(sucChua); }
    public void setTrangThai(String trangThai) { this.trangThai.set(trangThai); }
}