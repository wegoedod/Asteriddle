package byeolitgi.ui;

import byeolitgi.game.GameSession;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;

/** 시작 화면: 이어 하기 / 새 게임 / 종료. */
final class StartPanel extends JPanel {

    private final JButton resume = Theme.button("이어 하기");
    private final JLabel summary = new JLabel(" ");

    /** 제목과 이어 하기·새 게임·종료 버튼을 배치한다. */
    StartPanel(MainFrame frame) {
        setLayout(new GridBagLayout());

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        JLabel title = Theme.title("별잇기", 54);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("별의 배치를 보고 별자리를 유추해 직접 이어 보세요");
        subtitle.setFont(Theme.font(Font.PLAIN, 16));
        subtitle.setForeground(Theme.LABEL);
        summary.setForeground(new Color(170, 190, 230));
        summary.setFont(Theme.font(Font.PLAIN, 13));

        JButton newGame = Theme.button("새 게임");
        JButton quit = Theme.button("종료");
        resume.addActionListener(e -> frame.resumeGame());
        newGame.addActionListener(e -> frame.showSetup());
        quit.addActionListener(e -> frame.dispose());

        for (JComponent c : new JComponent[]{title, subtitle, summary, resume, newGame, quit}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        for (JButton b : new JButton[]{resume, newGame, quit}) {
            b.setMaximumSize(new Dimension(220, 40));
            b.setPreferredSize(new Dimension(220, 40));
            b.setFont(Theme.font(Font.BOLD, 15));
        }
        column.add(title);
        column.add(Box.createVerticalStrut(8));
        column.add(subtitle);
        column.add(Box.createVerticalStrut(36));
        column.add(resume);
        column.add(Box.createVerticalStrut(6));
        column.add(summary);
        column.add(Box.createVerticalStrut(14));
        column.add(newGame);
        column.add(Box.createVerticalStrut(10));
        column.add(quit);
        column.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(column);
    }

    /** 저장된 게임 요약과 이어 하기 버튼 상태를 갱신한다. */
    void refresh(GameSession session) {
        resume.setEnabled(session != null);
        summary.setText(session == null ? "저장된 게임이 없습니다" : session.summary());
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setPaint(new GradientPaint(0, 0, Theme.SKY_TOP, 0, getHeight(), Theme.SKY_BOTTOM));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
