package byeolitgi.ui;

import byeolitgi.model.Edge;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 별과 연결선을 그리는 캔버스.
 * 왼쪽 클릭: 별 선택/연결, 오른쪽 클릭: 선 삭제, 드래그: 이동, 휠: 확대·축소.
 */
public final class StarCanvas extends JPanel {

    public interface Listener {
        void starClicked(String starId);

        void emptyClicked();

        void edgeDeleteRequested(Edge edge);
    }

    /** 화면에 찍을 별. x, y 는 캔버스 중심 기준 단위 좌표(지름 2 = 짧은 변). */
    public record PlotStar(String id, double x, double y, double magnitude) {
    }

    /** 정답 도안 표시. title 이 있으면 구성 별 중심에 이름을 쓴다. */
    public record Overlay(Set<Edge> edges, Color color, String title) {
    }

    private static final double HIT_RADIUS = 11;
    private static final double EDGE_HIT = 6;

    private final Listener listener;
    private List<PlotStar> stars = List.of();
    private final Map<String, PlotStar> byId = new HashMap<>();
    private boolean horizon;
    private Set<Edge> userEdges = Set.of();
    private List<Overlay> overlays = List.of();
    private Map<String, String> starLabels = Map.of();
    private Edge hintEdge;
    private String selected;
    private Set<String> highlight = Set.of();
    private boolean editable = true;

    private double zoom = 1;
    /** 표시된 별 전체가 화면에 들어올 때까지 축소할 수 있다. */
    private double minZoom = 0.6;
    private double panX;
    private double panY;
    private Edge hoverEdge;
    private Point pressPoint;
    private double pressPanX;
    private double pressPanY;
    private boolean dragging;

    public StarCanvas(Listener listener) {
        this.listener = listener;
        setBackground(Theme.SKY_TOP);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressPoint = e.getPoint();
                pressPanX = panX;
                pressPanY = panY;
                dragging = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (pressPoint == null) {
                    return;
                }
                int dx = e.getX() - pressPoint.x;
                int dy = e.getY() - pressPoint.y;
                if (!dragging && Math.hypot(dx, dy) > 4) {
                    dragging = true;
                }
                if (dragging) {
                    panX = pressPanX + dx;
                    panY = pressPanY + dy;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean click = !dragging && pressPoint != null;
                pressPoint = null;
                dragging = false;
                if (!click) {
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e)) {
                    String star = starAt(e.getPoint());
                    if (star != null) {
                        listener.starClicked(star);
                    } else {
                        listener.emptyClicked();
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    Edge edge = edgeAt(e.getPoint());
                    if (edge != null) {
                        listener.edgeDeleteRequested(edge);
                    }
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Edge h = starAt(e.getPoint()) == null ? edgeAt(e.getPoint()) : null;
                if (!Objects.equals(h, hoverEdge)) {
                    hoverEdge = h;
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoomAt(e.getPoint(), Math.pow(1.15, -e.getPreciseWheelRotation()));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    // ------------------------------------------------------------------ 상태

    public void setStars(List<PlotStar> stars, boolean horizon) {
        this.stars = List.copyOf(stars);
        this.horizon = horizon;
        byId.clear();
        double extent = 0;
        for (PlotStar s : stars) {
            byId.put(s.id(), s);
            extent = Math.max(extent, Math.max(Math.abs(s.x()), Math.abs(s.y())));
        }
        minZoom = extent > 0 ? Math.min(0.6, 1 / extent) : 0.6;
        repaint();
    }

    public void setUserEdges(Set<Edge> edges) {
        this.userEdges = new LinkedHashSet<>(edges);
        if (hoverEdge != null && !userEdges.contains(hoverEdge)) {
            hoverEdge = null;
        }
        repaint();
    }

    public void setOverlays(List<Overlay> overlays) {
        this.overlays = List.copyOf(overlays);
        repaint();
    }

    public void setStarLabels(Map<String, String> labels) {
        this.starLabels = Map.copyOf(labels);
        repaint();
    }

    public void setHintEdge(Edge e) {
        this.hintEdge = e;
        repaint();
    }

    public void setSelected(String starId) {
        this.selected = starId;
        repaint();
    }

    public void setHighlight(Set<String> starIds) {
        this.highlight = Set.copyOf(starIds);
        repaint();
    }

    /** 편집할 수 없을 때는 삭제 대상 선 강조를 하지 않는다. */
    public void setEditable(boolean editable) {
        this.editable = editable;
        repaint();
    }

    public void resetView() {
        zoom = 1;
        panX = 0;
        panY = 0;
        repaint();
    }

    public void zoomBy(double factor) {
        zoomAt(new Point(getWidth() / 2, getHeight() / 2), factor);
    }

    private void zoomAt(Point p, double factor) {
        double newZoom = Math.max(minZoom, Math.min(12, zoom * factor));
        double cx = getWidth() / 2.0 + panX;
        double cy = getHeight() / 2.0 + panY;
        double mx = (p.x - cx) / unit();
        double my = (p.y - cy) / unit();
        zoom = newZoom;
        panX = p.x - getWidth() / 2.0 - mx * unit();
        panY = p.y - getHeight() / 2.0 - my * unit();
        repaint();
    }

    // ------------------------------------------------------------------ 좌표

    private double unit() {
        return Math.min(getWidth(), getHeight()) / 2.0 * 0.9 * zoom;
    }

    private Point2D toScreen(double x, double y) {
        return new Point2D.Double(getWidth() / 2.0 + panX + x * unit(), getHeight() / 2.0 + panY + y * unit());
    }

    private Point2D screen(PlotStar s) {
        return toScreen(s.x(), s.y());
    }

    private double radius(PlotStar s) {
        double r = Math.max(1.6, Math.min(7.0, 5.0 - 0.8 * s.magnitude()));
        return r * Math.max(0.6, Math.min(1.8, Math.pow(zoom, 0.25)));
    }

    private String starAt(Point p) {
        String best = null;
        double bestDist = Double.MAX_VALUE;
        for (PlotStar s : stars) {
            double d = screen(s).distance(p);
            if (d <= Math.max(HIT_RADIUS, radius(s) + 4) && d < bestDist) {
                best = s.id();
                bestDist = d;
            }
        }
        return best;
    }

    private Edge edgeAt(Point p) {
        if (!editable) {
            return null;
        }
        Edge best = null;
        double bestDist = EDGE_HIT;
        for (Edge e : userEdges) {
            PlotStar a = byId.get(e.a());
            PlotStar b = byId.get(e.b());
            if (a == null || b == null) {
                continue;
            }
            Point2D pa = screen(a);
            Point2D pb = screen(b);
            double d = Line2D.ptSegDist(pa.getX(), pa.getY(), pb.getX(), pb.getY(), p.x, p.y);
            if (d < bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, Theme.SKY_TOP, 0, getHeight(), Theme.SKY_BOTTOM));
        g.fillRect(0, 0, getWidth(), getHeight());

        if (horizon) {
            paintHorizon(g);
        }
        Stroke round = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g.setStroke(round);
        g.setColor(Theme.USER_LINE);
        for (Edge e : userEdges) {
            drawEdge(g, e);
        }
        if (hintEdge != null && userEdges.contains(hintEdge)) {
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Theme.HINT_LINE);
            drawEdge(g, hintEdge);
        }
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Overlay o : overlays) {
            g.setColor(withAlpha(o.color(), 210));
            for (Edge e : o.edges()) {
                drawEdge(g, e);
            }
        }
        if (hoverEdge != null) {
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Theme.DELETE_LINE);
            drawEdge(g, hoverEdge);
        }

        for (PlotStar s : stars) {
            paintStar(g, s);
        }
        paintLabels(g);
        g.dispose();
    }

    private void paintHorizon(Graphics2D g) {
        Point2D c = toScreen(0, 0);
        double r = unit();
        Ellipse2D dome = new Ellipse2D.Double(c.getX() - r, c.getY() - r, 2 * r, 2 * r);
        g.setColor(Theme.DOME);
        g.fill(dome);
        g.setColor(Theme.HORIZON);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(dome);
        g.setFont(Theme.font(Font.BOLD, 14));
        drawCentered(g, "북", toScreen(0, -1.06));
        drawCentered(g, "남", toScreen(0, 1.06));
        drawCentered(g, "동", toScreen(-1.06, 0));
        drawCentered(g, "서", toScreen(1.06, 0));
    }

    private void paintStar(Graphics2D g, PlotStar s) {
        Point2D p = screen(s);
        double r = radius(s);
        g.setColor(new Color(255, 255, 255, 35));
        g.fill(new Ellipse2D.Double(p.getX() - r * 2.2, p.getY() - r * 2.2, r * 4.4, r * 4.4));
        g.setColor(Theme.STAR);
        g.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r));
        if (highlight.contains(s.id())) {
            ring(g, p, r + 7, Theme.HIGHLIGHT);
        }
        if (s.id().equals(selected)) {
            ring(g, p, r + 5, Theme.SELECTED);
        }
    }

    private void paintLabels(Graphics2D g) {
        g.setFont(Theme.font(Font.PLAIN, 11));
        g.setColor(Theme.LABEL);
        for (Map.Entry<String, String> e : starLabels.entrySet()) {
            PlotStar s = byId.get(e.getKey());
            if (s != null) {
                Point2D p = screen(s);
                g.drawString(e.getValue(), (float) (p.getX() + radius(s) + 4), (float) (p.getY() + 4));
            }
        }
        g.setFont(Theme.font(Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        for (Overlay o : overlays) {
            if (o.title() == null) {
                continue;
            }
            Point2D c = titlePosition(o.edges(), fm.getHeight());
            if (c == null) {
                continue;
            }
            int w = fm.stringWidth(o.title()) + 14;
            int h = fm.getHeight() + 4;
            RoundRectangle2D box = new RoundRectangle2D.Double(c.getX() - w / 2.0, c.getY() - h / 2.0, w, h, 10, 10);
            g.setColor(new Color(10, 14, 30, 190));
            g.fill(box);
            g.setColor(o.color());
            g.draw(box);
            drawCentered(g, o.title(), c);
        }
    }

    /** 별 이름을 가리지 않도록 도안 아래쪽 가운데에 이름을 둔다. */
    private Point2D titlePosition(Set<Edge> edges, int textHeight) {
        Set<String> ids = new LinkedHashSet<>();
        for (Edge e : edges) {
            ids.add(e.a());
            ids.add(e.b());
        }
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (String id : ids) {
            PlotStar s = byId.get(id);
            if (s != null) {
                Point2D p = screen(s);
                minX = Math.min(minX, p.getX());
                maxX = Math.max(maxX, p.getX());
                maxY = Math.max(maxY, p.getY());
            }
        }
        return maxY == -Double.MAX_VALUE ? null : new Point2D.Double((minX + maxX) / 2, maxY + textHeight + 6);
    }

    private void drawEdge(Graphics2D g, Edge e) {
        PlotStar a = byId.get(e.a());
        PlotStar b = byId.get(e.b());
        if (a == null || b == null) {
            return;
        }
        g.draw(new Line2D.Double(screen(a), screen(b)));
    }

    private static void ring(Graphics2D g, Point2D p, double r, Color color) {
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2f));
        g.setColor(color);
        g.draw(new Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r));
        g.setStroke(old);
    }

    private static void drawCentered(Graphics2D g, String text, Point2D p) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (p.getX() - fm.stringWidth(text) / 2.0),
                (float) (p.getY() + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
