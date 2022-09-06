package org.suancai.toolbag.lib1236jdbc;

import org.suancai.toolbag.common.exception.Duplicate_exception;
import org.suancai.toolbag.common.exception.Not_exist_exception;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author: SuricSun
 * @date: 2022/1/16
 */
public class Conn_wrapper {

    private Connection conn = null;
    private PreparedStatement stmt = null;
    private List<Savepoint> savepoint_list = null;

    public Conn_wrapper(Connection _conn) throws SQLException {

        // * make sure the auto-commit is disabled
        this.conn = _conn;
        this.conn.setAutoCommit(false);
        this.savepoint_list = new ArrayList<>();
    }

    /**
     * 在事务中设置一个保存点, 以用roll_back_to方法回退到某个特定的保存点
     *
     * @return 返回自己以进行链式调用
     * @throws SQLException
     * @throws Duplicate_exception 如果savepoint名在此次事务中已存在
     */
    public Conn_wrapper set_savepoint(String name) throws SQLException, Duplicate_exception {

        //检查是否存在
        // * NEVER GONNA HAPPEN
        //如果遇上一个unnamed的保存点，直接返回不匹配
        if (this.savepoint_list.stream().anyMatch((sp) -> {
            try {
                return sp.getSavepointName().equals(name);
            } catch (SQLException e) {
                // * NEVER GONNA HAPPEN
                e.printStackTrace();
            }
            // * NEVER GONNA HAPPEN
            //如果真遇上一个unnamed的保存点，直接返回不匹配
            return false;
        })) {
            //已有同名
            throw new Duplicate_exception("Savepoint duplicated: \"" + name + "\"");
        }

        this.savepoint_list.add(this.conn.setSavepoint(name));
        return this;
    }

    public Conn_wrapper rollback_to_savepoint(String name) throws Not_exist_exception, SQLException {

        Optional<Savepoint> op_sp = this.savepoint_list
                .stream()
                .filter((sp) -> {
                    try {
                        return sp.getSavepointName().equals(name);
                    } catch (SQLException e) {
                        // * NEVER GONNA HAPPEN
                        e.printStackTrace();
                    }
                    // * NEVER GONNA HAPPEN
                    //如果真遇上一个unnamed的保存点，直接返回不匹配
                    return false;
                })
                .findFirst();

        if (op_sp.isPresent()) {
            this.conn.rollback(op_sp.get());
        } else {
            throw new Not_exist_exception("Savepoint does not exist: " + name);
        }

        return this;
    }

    public Conn_wrapper rollback_to_nearest_savepoint() throws SQLException {

        this.conn.rollback(this.savepoint_list.get(this.savepoint_list.size() - 1));
        return this;
    }

    /**
     *
     */
    private void release_all_savepoint() throws SQLException {

        if (this.savepoint_list.size() > 0) {
            this.conn.releaseSavepoint(this.savepoint_list.get(0));
        }
    }

    /**
     * 提交一个事务
     *
     * @return 返回自己以进行链式调用
     * @throws SQLException
     */
    public Conn_wrapper commit_tran() throws SQLException {

        this.conn.commit();
        this.release_all_savepoint();
        return this;
    }

    /**
     * 回滚事务到最初点
     *
     * @return 返回自己以进行链式调用
     * @throws SQLException
     */
    public Conn_wrapper undo_tran() throws SQLException {

        this.conn.rollback();
        this.release_all_savepoint();
        return this;
    }

    public Conn_wrapper use_db(String db_name) throws SQLException {

        this.set_stmt("use " + db_name).exec_stmt();
        return this;
    }

    /**
     * @param tblName 要插入的表名
     * @param data    插入的实体类对象
     * @return 返回结果集包装器
     * @throws IllegalAccessException
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public Result_wrapper insert_into_table(String tblName, Object data) throws IllegalAccessException, SQLException {

        Field[] fields = null;
        if (data instanceof List<?>) {
            fields = ((Class<?>) (((List<?>) data).getClass().getGenericSuperclass())).getDeclaredFields();
        } else {
            fields = data.getClass().getDeclaredFields();
            Object tmp = data;
            data = new ArrayList<>();
            ((ArrayList<Object>) data).add(tmp);
        }
        //生成SQl语句
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tblName).append("(");
        List<Integer> accFieldIdx = new ArrayList<>();
        boolean hasPre = false;
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (f.getType() == String.class
                    || f.getType() == Integer.class
                    || f.getType() == Date.class
                    || f.getType() == Double.class) {
                accFieldIdx.add(i);
                if (hasPre) {
                    sb.append(',');
                } else {
                    hasPre = true;
                }
                sb.append(f.getName().toLowerCase());
            }
        }
        sb.append(")VALUES");
        boolean another_has_pre = false;
        for (Object obj : (List<Object>) data) {
            if (another_has_pre) {
                sb.append(',');
            } else {
                another_has_pre = true;
            }
            sb.append('(');
            hasPre = false;
            for (Integer i : accFieldIdx) {
                Field f = fields[i];
                f.setAccessible(true);
                if (hasPre) {
                    sb.append(',');
                } else {
                    hasPre = true;
                }
                if (f.getType() == String.class) {
                    sb.append('\"').append(f.get(obj).toString()).append('\"');
                } else if (f.getType() == Integer.class) {
                    sb.append(f.get(obj).toString());
                } else if (f.getType() == Date.class) {
                    sb.append('\"').append(f.get(obj).toString()).append('\"');
                } else if (f.getType() == Double.class) {
                    sb.append(f.get(obj).toString());
                }
            }
            sb.append(')');
        }
        return this.set_stmt(sb.toString()).exec_stmt();
    }

    /**
     * 设置待执行的SQL语句
     *
     * @param stmt_str SQL语句
     * @return 返回自己以进行链式调用
     * @throws SQLException
     */
    public Conn_wrapper set_stmt(String stmt_str) throws SQLException {

        this.stmt = this.conn.prepareStatement(stmt_str);
        return this;
    }

    /**
     * 设置待执行的SQL语句，指定ResultSet类型和Concurrency类型
     *
     * @param stmt_str               SQL语句
     * @param result_set_type        结果集类型
     * @param result_set_concurrency 一致性类型
     * @return 返回自己以进行链式调用
     * @throws SQLException
     */
    public Conn_wrapper set_stmt(String stmt_str, int result_set_type, int result_set_concurrency) throws SQLException {

        this.stmt = this.conn.prepareStatement(stmt_str, result_set_type, result_set_concurrency);
        return this;
    }

    /**
     * 对于之前调用{@link Conn_wrapper#set_stmt(String)}或{@link Conn_wrapper#set_stmt(String, int, int)}设置的SQL语句,
     * 设置SQL参数.
     * 如果传入的Array中有数组元素是list类型, 函数会把list里面的object全部算进去
     *
     * @param args 参数数组
     * @return 返回自己以进行链式调用
     * @throws SQLException
     */
    public Conn_wrapper with_args(Object... args) throws SQLException {

        int cnt = 1;
        for (Object arg : args) {
            if (arg instanceof List) {
                List<?> li = (List<?>) arg;
                for (Object e : li) {
                    this.with_args(cnt, e);
                    cnt++;
                }
            } else {
                this.with_args(cnt, arg);
                cnt++;
            }
        }
        return this;
    }

    /**
     * 对于之前调用{@link Conn_wrapper#set_stmt(String)}或{@link Conn_wrapper#set_stmt(String, int, int)}设置的SQL语句,
     * 设置SQL参数.
     *
     * @param idx 从1开始
     * @param arg 参数
     * @return 返回自己以进行链式调用
     * @throws SQLException
     */
    public Conn_wrapper with_args(int idx, Object arg) throws SQLException {

        if (arg instanceof String) {
            this.stmt.setString(idx, (String) arg);
        } else if (arg instanceof Integer) {
            this.stmt.setInt(idx, (Integer) arg);
        } else if (arg instanceof java.sql.Date) {
            this.stmt.setDate(idx, (java.sql.Date) arg);
        } else if (arg instanceof Float) {
            this.stmt.setFloat(idx, (Float) arg);
        } else if (arg instanceof Double) {
            this.stmt.setDouble(idx, (Double) arg);
        } else if (arg instanceof Timestamp) {
            this.stmt.setTimestamp(idx, (Timestamp) arg);
        } else {
            throw new SQLException("Unsupported arg type: " + arg.getClass().getName());
        }
        return this;
    }

    /**
     * 执行设置的语句
     *
     * @return 返回结果集包装器
     * @throws SQLException
     */
    public Result_wrapper exec_stmt() throws SQLException {

        this.stmt.execute();
        Result_wrapper resultWrapper = new Result_wrapper();
        resultWrapper.set_result_set(this.stmt.getResultSet());
        resultWrapper.set_update_cnt(this.stmt.getUpdateCount());
        return resultWrapper;
    }

    /**
     * 关闭此次sql连接
     */
    public void close() {

        try {
            if (this.stmt != null) {
                this.stmt.close();
            }
            if (this.conn != null) {
                this.conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
