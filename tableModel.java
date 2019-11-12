package FileSystem;

import javax.swing.table.AbstractTableModel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * Created by hongjiayong on 16/6/7.
 */
public class tableModel extends AbstractTableModel {
    private Vector content = null;
    private String[] title_name = { "File Name", "File Path", "File Type", "File Volume/KB", "Last Update"};

    public tableModel(){
        content = new Vector();
    }

    public void addRow(FCB fcb){
        Vector v = new Vector();
        DecimalFormat format=new DecimalFormat("#0");
        v.add(0, fcb.getFileName());
        v.add(1, fcb.getFilePath());
        if (fcb.getMyFile().isFile()){
            v.add(2, "File");
            if (fcb.getSpace() % 1 == 0){// 是这个整数，小数点后面是0
                v.add(3, (int)fcb.getSpace());
            }else {//不是整数，小数点后面不是0
                v.add(3, (int) fcb.getSpace() + 1);
            }
        }else {
            v.add(2, fcb.getType());
            v.add(3, "-");
        }

        String ctime = fcb.getModifiedTime();
        v.add(4, ctime);
        content.add(v);
    }

    public void removeRow(String name) {
        for (int i = 0; i < content.size(); i++){
            if (((Vector)content.get(i)).get(0).equals(name)){
                content.remove(i);
                break;
            }
        }
    }

    public void removeRows(int row, int count){
        for (int i = 0; i < count; i++){
            if (content.size() > row){
                content.remove(row);
            }
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int colIndex){
        ((Vector) content.get(rowIndex)).remove(colIndex);
        ((Vector) content.get(rowIndex)).add(colIndex, value);
        this.fireTableCellUpdated(rowIndex, colIndex);
    }

    public String getColumnName(int col) {
        return title_name[col];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex){
        return false;
    }

    @Override
    public int getRowCount() {
        return content.size();
    }

    @Override
    public int getColumnCount() {
        return title_name.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return ((Vector) content.get(rowIndex)).get(columnIndex);
    }
}
