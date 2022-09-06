import org.junit.jupiter.api.Test;
import org.suancai.toolbag.lib1236jdbc.Conn_wrapper;
import org.suancai.toolbag.lib1236jdbc.JDBC_wrapper_Factory;
import org.suancai.toolbag.lib1236jdbc.Result_wrapper;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

/**
 * @author: SuricSun
 * @date: 2022/9/5
 */
public class TestCase {

    @Test
    public void test() throws Exception {

        System.out.println("START");
        JDBC_wrapper_Factory.Init_sql_driver("com.mysql.cj.jdbc.Driver");
        JDBC_wrapper_Factory jdbc_wrapper_fac =
                new JDBC_wrapper_Factory(
                        "com.mysql.cj.jdbc.Driver",
                        "jdbc:mysql://localhost:7521?characterEncoding=utf8&serverTimezone=UTC",
                        "root",
                        "7521");

        Conn_wrapper cw = jdbc_wrapper_fac.get_conn_wrapper();

//        Result_wrapper rw = cw
//                .use_db("scy")
//                .set_stmt("insert into book values ('我是傻逼','awdawdawd',0,0)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
//                .exec_stmt();
//
//        cw.set_savepoint("0");
//
//        rw = cw
//                .use_db("scy")
//                .set_stmt("insert into book values ('我是傻逼0','awdawdawd',0,0)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
//                .exec_stmt();
//
//        cw.set_savepoint("1");
//
//        rw = cw
//                .use_db("scy")
//                .set_stmt("insert into book values ('我是傻逼1','awdawdawd',0,0)", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
//                .exec_stmt();
//
//
//        cw.rollback_to_nearest_savepoint();
//
//        cw.commit_tran();
//
        Result_wrapper rw = cw
                .use_db("scy")
                .set_stmt("select * from book", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
                .exec_stmt();

        List<Entity> li = rw.result_set_to_list(Entity.class);
        System.out.println(Arrays.toString(li.toArray()));

        rw.reset_iterate_state();
        li = rw.result_set_to_list(Entity.class);
        System.out.println(Arrays.toString(li.toArray()));

        li = rw.result_set_to_list(Entity.class);
        System.out.println(Arrays.toString(li.toArray()));

        Entity entity = rw.result_set_to_single_object(Entity.class);
        System.out.println(entity);
    }
}

class Entity {

    public String bookName = "NONE";
    public String bookDesc = "NONE";

    @Override
    public String toString() {
        return "\n " + this.bookName + ", " + this.bookDesc;
    }
}