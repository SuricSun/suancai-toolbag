package org.suancai.toolbag.lib1236jdbc;

import org.suancai.toolbag.lib1236jdbc.exception.Convert_exception;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: SuricSun
 * @date: 2022/1/16
 */
public class Result_wrapper {

    private Integer updateCnt = 0;
    private ResultSet resultSet = null;

    /**
     * 只转换结果集合的第一个元素
     *
     * @param outType outType
     * @param <T>     <T>
     * @return 没有元素就返回null
     */

    public <T> T result_set_to_single_object(Class<T> outType) throws Exception {

        try {
            Field[] fields = outType.getDeclaredFields();
            Constructor<T> constructor = outType.getDeclaredConstructor();
            constructor.setAccessible(true);
            T out = constructor.newInstance();
            ResultSetMetaData metaData = this.resultSet.getMetaData();
            int[] fieldToColumn = new int[fields.length];
            //对于每个field
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                if (Modifier.isPublic(f.getModifiers())) {
                    f.setAccessible(true);
                    //对于所有列名
                    int j = 0;
                    for (j = 0; j < metaData.getColumnCount(); j++) {
                        if (metaData.getColumnName(j + 1).equalsIgnoreCase(f.getName())) {
                            fieldToColumn[i] = j + 1;
                            break;
                        }
                    }
                    if (j >= metaData.getColumnCount()) {
                        fieldToColumn[i] = 0;
                    }
                }
            }
            //开始init
            if (this.resultSet.next() == false) {
                //如果没有next
                return null;
            }
            for (int i = 0; i < fields.length; i++) {
                if (fieldToColumn[i] == 0) {
                    continue;
                }
                Field f = fields[i];
                f.set(out, this.resultSet.getObject(fieldToColumn[i]));
            }
            return out;
        } catch (Exception e) {
            throw new Convert_exception(e);
        }
    }

    /**
     * @param outType outType
     * @param <T>     <T>
     * @return 没有元素就返回一个空表
     * @throws Convert_exception none
     */
    public <T> List<T> result_set_to_list(Class<T> outType) throws Convert_exception {

        try {
            List<T> li = new ArrayList<>();
            Field[] fields = outType.getDeclaredFields();
            ResultSetMetaData metaData = this.resultSet.getMetaData();
            int[] fieldToColumn = new int[fields.length];
            //对于每个field
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                if (Modifier.isPublic(f.getModifiers())) {
                    f.setAccessible(true);
                    //对于所有列名
                    int j = 0;
                    for (j = 0; j < metaData.getColumnCount(); j++) {
                        if (metaData.getColumnName(j + 1).equalsIgnoreCase(f.getName())) {
                            fieldToColumn[i] = j + 1;
                            break;
                        }
                    }
                    if (j >= metaData.getColumnCount()) {
                        fieldToColumn[i] = 0;
                    }
                }
            }
            //开始init
            while (this.resultSet.next()) {
                Constructor<T> constructor = outType.getDeclaredConstructor();
                constructor.setAccessible(true);
                li.add(constructor.newInstance());
                for (int i = 0; i < fields.length; i++) {
                    if (fieldToColumn[i] == 0) {
                        continue;
                    }
                    Field f = fields[i];
                    Object tmp = li.get(li.size() - 1);
                    f.set(tmp, this.resultSet.getObject(fieldToColumn[i]));
                }
            }
            return li;
        } catch (Exception e) {
            throw new Convert_exception(e);
        }
    }

    public void reset_iterate_state() throws SQLException {

        this.resultSet.beforeFirst();
    }

    public Integer get_update_cnt() {

        return updateCnt;
    }

    public void set_update_cnt(Integer updateCnt) {

        this.updateCnt = updateCnt;
    }

    public ResultSet get_result_set() {

        return resultSet;
    }

    public void set_result_set(ResultSet resultSet) {

        this.resultSet = resultSet;
    }
}
