
import javax.swing.*;
import java.awt.*;

public class Calculator {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Java Calculator");
        frame.setSize(350, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(5, 2, 10, 10));

        JTextField num1 = new JTextField();
        JTextField num2 = new JTextField();

        String[] operations = {
            "Addition (+)",
            "Subtraction (-)",
            "Multiplication (*)",
            "Division (/)"
        };

        JComboBox<String> operation = new JComboBox<>(operations);

        JButton calculate = new JButton("Calculate");

        JLabel result = new JLabel("Result: ");

        frame.add(new JLabel("First Number:"));
        frame.add(num1);

        frame.add(new JLabel("Second Number:"));
        frame.add(num2);

        frame.add(new JLabel("Operation:"));
        frame.add(operation);

        frame.add(calculate);
        frame.add(result);

        calculate.addActionListener(e -> {

            try {
                double a = Double.parseDouble(num1.getText());
                double b = Double.parseDouble(num2.getText());

                double answer = 0;

                switch (operation.getSelectedIndex()) {

                    case 0:
                        answer = a + b;
                        break;

                    case 1:
                        answer = a - b;
                        break;

                    case 2:
                        answer = a * b;
                        break;

                    case 3:
                        if (b == 0) {
                            JOptionPane.showMessageDialog(
                                frame,
                                "Cannot divide by zero!"
                            );
                            return;
                        }
                        answer = a / b;
                        break;
                }

                result.setText("Result: " + answer);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Please enter valid numbers!"
                );
            }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
