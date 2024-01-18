import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 类名: Register
 * 类功能: 注册
 */
public class Register extends JFrame {
    public static void main(String[] args) {
        new Register();
    }

    public Register(){
        setTitle("ChatRoom");
        BufferedImage img;
        try {
            img = ImageIO.read(Server.class.getResource("/ChatRoom.png"));
            setIconImage(img);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        setLayout(null);
        setSize(320,480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // 用户头像面板
        ImagePanel imagePanel=new ImagePanel();
        Image image = Toolkit.getDefaultToolkit().getImage("media/register_icon.png");
        imagePanel.paintImage(image);
        imagePanel.setBounds(110,5,100,100);
        add(imagePanel);

        // 用户名标题
        JLabel username_label = new JLabel("用户名");
        username_label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        username_label.setBounds(55,90,100,50);
        add(username_label);

        // 输入用户名的文本区域
        JTextField username_field = new JTextField();
        username_field.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        username_field.setBounds(52,125,216,35);
        add(username_field);

        // 密码标题
        JLabel password_label = new JLabel("密码");
        password_label.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        password_label.setBounds(55,150,100,50);
        add(password_label);

        // 输入密码的文本区域
        JPasswordField password_field = new JPasswordField();
        password_field.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        password_field.setBounds(52,185,216,35);
        add(password_field);

        // 请再次输入密码标题
        JLabel password_label2 = new JLabel("请再次输入密码");
        password_label2.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        password_label2.setBounds(55,210,100,50);
        add(password_label2);

        // 输入请再次输入密码的文本区域
        JPasswordField password_field2 = new JPasswordField();
        password_field2.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        password_field2.setBounds(52,245,216,35);
        add(password_field2);

        // 注册按钮
        JButton register_success = new JButton("注册");
        register_success.setBounds(105,300,110,40);
        register_success.setFont(new Font("微软雅黑",Font.PLAIN,15));
        register_success.setBackground(new Color(0,122,255));
        add(register_success);

        // 返回按钮
        JButton back = new JButton("返回");
        back.setBounds(105,350,110,40);
        back.setFont(new Font("微软雅黑",Font.PLAIN,15));
        add(back);

        setVisible(true);

        // 注册按钮点击事件
        register_success.addActionListener(e -> {
            String username = username_field.getText();
            String password = String.valueOf(password_field.getPassword());
            String password2 = String.valueOf(password_field2.getPassword());
            System.out.println("注册密码：" + password);
            System.out.println("确认注册密码：" + password2);
            if(username.isEmpty() || password.isEmpty()){
                ImageIcon icon = new ImageIcon("media/notice.png");//图片的大小需要调整到合适程度
                JOptionPane.showMessageDialog(null, "注册失败\n账号或密码不能为空，请重试","提示",JOptionPane.ERROR_MESSAGE,icon);

            }else if (!password.equals(password2)) {
                ImageIcon icon = new ImageIcon("media/notice.png");//图片的大小需要调整到合适程度
                JOptionPane.showMessageDialog(null, "注册失败\n两次输入密码不匹配，请重试","提示",JOptionPane.ERROR_MESSAGE,icon);

            }else{
                User user = new User();
                user.setUsername(username);
                user.setPassword(password);
                UserDao dao = new UserDao();
                int flag = dao.register(user);
                if(flag != 0){
                    System.out.println("用户：" + username + " 注册成功," + "密码：" + password);
                    ImageIcon icon = new ImageIcon("media/success.png");//图片的大小需要调整到合适程度
                    JOptionPane.showMessageDialog(null, "注册成功\n欢迎登录","提示",JOptionPane.ERROR_MESSAGE,icon);
                    setVisible(false);
                    new Client();

                }else{
                    ImageIcon icon = new ImageIcon("media/notice.png");//图片的大小需要调整到合适程度
                    JOptionPane.showMessageDialog(null, "注册失败\n用户名已经存在，请重试","提示",JOptionPane.ERROR_MESSAGE,icon);

                }
            }
        });

        // 返回按钮点击事件
        back.addActionListener(e ->{
            System.out.println("用户从注册页面返回登录页面");
            setVisible(false);
            new Client();
        });

    }

}


