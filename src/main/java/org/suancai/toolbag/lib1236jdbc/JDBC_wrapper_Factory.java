package org.suancai.toolbag.lib1236jdbc;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author: SuricSun
 * @date: 2022/1/16
 */
public class JDBC_wrapper_Factory {

    private String driver_name;
    private String url;
    private String user_name;
    private String password;

    public JDBC_wrapper_Factory(String driver_name, String url, String user_name, String password) throws ClassNotFoundException {

        this.driver_name = driver_name;
        this.url = url;
        this.user_name = user_name;
        this.password = password;
        //确保Connector-Java一定被加载而已
        JDBC_wrapper_Factory.Init_sql_driver(this.driver_name);
    }

    public Conn_wrapper get_conn_wrapper() throws SQLException {

        return new Conn_wrapper(DriverManager.getConnection(this.url, this.user_name, this.password));
    }

    /**
     * 确保Connector-Java一定被加载而已
     * @param driver_name driver_name
     * @throws ClassNotFoundException
     */
    public static void Init_sql_driver(String driver_name) throws ClassNotFoundException {

        Class.forName(driver_name);
    }
}
