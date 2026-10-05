package byeolitgi.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collections;

/** 화면 색상과 글꼴. 선택한 별·연결선·판정 결과를 색으로 구별한다 (NFR-01). */
public final class Theme {

    public static final Color SKY_TOP = new Color(8, 12, 32);
    public static final Color SKY_BOTTOM = new Color(18, 26, 58);
    public static final Color DOME = new Color(16, 26, 60);
    public static final Color HORIZON = new Color(90, 120, 190);
    public static final Color STAR = new Color(250, 250, 255);
    public static final Color USER_LINE = new Color(120, 190, 255);
    public static final Color HINT_LINE = new Color(255, 170, 60);
    public static final Color SOLVED_LINE = new Color(90, 220, 130);
    public static final Color REVEALED_LINE = new Color(200, 140, 255);
    public static final Color DELETE_LINE = new Color(255, 90, 90);
    public static final Color SELECTED = new Color(255, 225, 80);
    public static final Color HIGHLIGHT = new Color(90, 230, 255);
    public static final Color LABEL = new Color(200, 210, 235);

    public static final Color OK = new Color(30, 140, 70);
    public static final Color ERROR = new Color(200, 50, 50);
    public static final Color INFO = new Color(40, 70, 140);

    private static String family = Font.DIALOG;

    private Theme() {
    }

    /** 한글이 잘 보이는 글꼴을 기본 글꼴로 지정한다. */
    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 기본 룩앤필 사용
        }
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String candidate : new String[]{"Malgun Gothic", "맑은 고딕", "Apple SD Gothic Neo", "NanumGothic"}) {
            if (Arrays.asList(available).contains(candidate)) {
                family = candidate;
                break;
            }
        }
        FontUIResource base = new FontUIResource(family, Font.PLAIN, 13);
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, base);
            }
        }
    }

    public static Font font(int style, int size) {
        return new Font(family, style, size);
    }

    public static JLabel title(String text, int size) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, size));
        return l;
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setMargin(new Insets(6, 12, 6, 12));
        return b;
    }
}
