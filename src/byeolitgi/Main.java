package byeolitgi;

import byeolitgi.data.CatalogLoader;
import byeolitgi.model.Catalog;
import byeolitgi.save.SaveManager;
import byeolitgi.ui.MainFrame;
import byeolitgi.ui.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 별잇기 실행 진입점. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            Catalog catalog;
            try {
                catalog = CatalogLoader.loadDefault();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "별자리 데이터를 불러오지 못했습니다.\n" + e.getMessage(),
                        "별잇기", JOptionPane.ERROR_MESSAGE);
                return;
            }
            MainFrame frame = new MainFrame(catalog, new SaveManager(SaveManager.defaultPath()));
            frame.setVisible(true);
            frame.boot();
        });
    }
}
