import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 类名: UserDao
 * 类功能: 连接 MySQL 数据库，实现用户登陆 or 注册
 */
public class UserDao {
    private final JdbcTemplate template = new JdbcTemplate(JDBCUtils.getDataSource());

    /**
     * 函数名称: login
     * 函数功能: 登陆
     * @param login_user: 用户名
     * @return: the single mapped object
     */
    public User login(User login_user) {
        try {
            String sql = "select * from user where username = ? and password = ?";
            return template.queryForObject(sql,
                    new BeanPropertyRowMapper<>(User.class),
                    login_user.getUsername(), login_user.getPassword());

        } catch (DataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 函数名称: register
     * @param register_user: 用户名
     * @return: the number of rows affected
     */
    public int register(User register_user) {
        try {
            String sql = "insert into user values (?,?)";
            return template.update(sql,register_user.getUsername(),register_user.getPassword());

        } catch (DataAccessException e) {
            e.printStackTrace();
            return 0;
        }
    }

}


