package fraudguard;

import fraudguard.ui.FraudGuardFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FraudGuardFrame().setVisible(true));
    }
}