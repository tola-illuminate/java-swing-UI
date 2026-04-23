package view;


import javax.swing.*;
import java.awt.*;
import util.MeStyle;

public class DashboardFrame extends JFrame {

    public DashboardFrame() {
        setTitle("TaskFlow Dashboard");
        setSize(1100, 720);


        getContentPane().setBackground(MeStyle.BG);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());


        setVisible(true);
    }
}