import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;


public class SmartTaskSchedulerSwing extends JFrame {

    private PriorityQueue<Task> taskQueue = new PriorityQueue<>();
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> taskList = new JList<>(listModel);
    private final String FILE_NAME = "tasks.txt";

    private JTextField titleField;
    private JComboBox<String> priorityBox;
    private JTextField deadlineField;

    public SmartTaskSchedulerSwing() {
        super("Smart Task Scheduler (Swing)");
        setSize(950, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new FlowLayout());

        titleField = new JTextField(15);
        priorityBox = new JComboBox<>(new String[]{"1 - High", "2 - Medium", "3 - Low"});
        deadlineField = new JTextField(10);
        JButton addButton = new JButton("Add Task");
        JButton deleteButton = new JButton("Delete Task");
        JButton saveButton = new JButton("Save");

        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Priority:"));
        inputPanel.add(priorityBox);
        inputPanel.add(new JLabel("Deadline (yyyy-mm-dd):"));
        inputPanel.add(deadlineField);
        inputPanel.add(addButton);
        inputPanel.add(deleteButton);
        inputPanel.add(saveButton);

        JPanel filterPanel = new JPanel();

        JButton showAllButton = new JButton("Show All");
        JButton filterTodayButton = new JButton("Today's Tasks");
        JButton filterHighButton = new JButton("High Priority");
        JButton showDueSoonButton = new JButton("Show Due Soon");

        JTextField searchField = new JTextField(10);
        JButton searchButton = new JButton("Search");

        JButton sortAscButton = new JButton("Sort by Deadline ↑");
        JButton sortDescButton = new JButton("Sort by Deadline ↓");

        filterPanel.add(showAllButton);
        filterPanel.add(filterTodayButton);
        filterPanel.add(filterHighButton);
        filterPanel.add(showDueSoonButton);
        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        filterPanel.add(sortAscButton);
        filterPanel.add(sortDescButton);

        JScrollPane listScroll = new JScrollPane(taskList);

        add(inputPanel, BorderLayout.NORTH);
        add(filterPanel, BorderLayout.SOUTH);
        add(listScroll, BorderLayout.CENTER);


        taskList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String val = value.toString();
                try {
                    String[] parts = val.split("Due: ");
                    if (parts.length == 2) {
                        LocalDate dueDate = LocalDate.parse(parts[1].trim());
                        LocalDate today = LocalDate.now();

                        if (dueDate.isBefore(today)) {
                            label.setForeground(Color.RED); 
                        } else if (!dueDate.isAfter(today.plusDays(5))) {
                            label.setForeground(new Color(255, 140, 0)); 
                        } else {
                            label.setForeground(Color.BLACK);
                        }
                    }
                } catch (Exception e) {
                    label.setForeground(Color.BLACK);
                }
                return label;
            }
        });


        loadTasks();
        updateList();


        addButton.addActionListener(e -> {
            String title = titleField.getText();
            int priority = priorityBox.getSelectedIndex() + 1;
            String deadlineStr = deadlineField.getText();
            try {
                LocalDate deadline = LocalDate.parse(deadlineStr);
                if (!title.isEmpty()) {
                    taskQueue.add(new Task(title, priority, deadline));
                    updateList();
                    titleField.setText("");
                    deadlineField.setText("");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid deadline format. Use yyyy-mm-dd.");
            }
        });

        deleteButton.addActionListener(e -> {
            String selected = taskList.getSelectedValue();
            if (selected != null) {
                taskQueue.removeIf(t -> t.toString().equals(selected));
                updateList();
            }
        });

        saveButton.addActionListener(e -> saveTasks());

        showAllButton.addActionListener(e -> updateList());

        filterTodayButton.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            listModel.clear();
            taskQueue.stream()
                .filter(t -> t.getDeadline().equals(today))
                .sorted()
                .forEach(t -> listModel.addElement(t.toString()));
        });

        filterHighButton.addActionListener(e -> {
            listModel.clear();
            taskQueue.stream()
                .filter(t -> t.getPriority() == 1)
                .sorted()
                .forEach(t -> listModel.addElement(t.toString()));
        });

        showDueSoonButton.addActionListener(e -> {
            LocalDate today = LocalDate.now();
            LocalDate soonLimit = today.plusDays(5);
            listModel.clear();
            taskQueue.stream()
                .filter(t -> !t.getDeadline().isBefore(today) && !t.getDeadline().isAfter(soonLimit))
                .sorted()
                .forEach(t -> listModel.addElement(t.toString()));
        });

        searchButton.addActionListener(e -> {
            String query = searchField.getText().toLowerCase();
            listModel.clear();
            taskQueue.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(query))
                .sorted()
                .forEach(t -> listModel.addElement(t.toString()));
        });

        sortAscButton.addActionListener(e -> {
            List<Task> sorted = new ArrayList<>(taskQueue);
            sorted.sort(Comparator.comparing(Task::getDeadline));
            listModel.clear();
            sorted.forEach(t -> listModel.addElement(t.toString()));
        });

        sortDescButton.addActionListener(e -> {
            List<Task> sorted = new ArrayList<>(taskQueue);
            sorted.sort(Comparator.comparing(Task::getDeadline).reversed());
            listModel.clear();
            sorted.forEach(t -> listModel.addElement(t.toString()));
        });

        startReminder();
    }

    private void updateList() {
        listModel.clear();
        taskQueue.stream()
            .sorted()
            .forEach(t -> listModel.addElement(t.toString()));
    }

    private void saveTasks() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Task task : taskQueue) {
                writer.println(task.getTitle() + "|" + task.getPriority() + "|" + task.getDeadline());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving tasks: " + e.getMessage());
        }
    }

    private void loadTasks() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 3) {
                        String title = parts[0];
                        int priority = Integer.parseInt(parts[1].trim());
                        LocalDate deadline = LocalDate.parse(parts[2].trim());
                        taskQueue.add(new Task(title, priority, deadline));
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage());
            }
        }
    }

    private void startReminder() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                for (Task task : taskQueue) {
                    if (task.getDeadline().equals(LocalDate.now())) {
                        System.out.println("🔔 Reminder: " + task.getTitle() + " is due today!");
                    }
                }
            }
        }, 0, 60 * 1000);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SmartTaskSchedulerSwing().setVisible(true));
    }

    public static class Task implements Comparable<Task> {
        private String title;
        private int priority;
        private LocalDate deadline;

        public Task(String title, int priority, LocalDate deadline) {
            this.title = title;
            this.priority = priority;
            this.deadline = deadline;
        }

        public String getTitle() { return title; }
        public int getPriority() { return priority; }
        public LocalDate getDeadline() { return deadline; }

        @Override
        public int compareTo(Task other) {
            if (this.priority != other.priority)
                return Integer.compare(this.priority, other.priority);
            return this.deadline.compareTo(other.deadline);
        }

        @Override
        public String toString() {
            String p = switch (priority) {
                case 1 -> "High";
                case 2 -> "Medium";
                default -> "Low";
            };
            return title + " | Priority: " + p + " | Due: " + deadline;
        }
    }
}
