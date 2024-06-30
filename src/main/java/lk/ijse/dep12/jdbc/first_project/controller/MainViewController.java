package lk.ijse.dep12.jdbc.first_project.controller;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import lk.ijse.dep12.jdbc.first_project.db.SingletonConnection;
import lk.ijse.dep12.jdbc.first_project.to.Student;

import java.sql.*;

public class MainViewController {

    public Button btnDelete;

    public Button btnNewStudent;

    public Button btnSave;

    public TableView<Student> tblStudent;

    public TextField txtAddress;

    public TextField txtContact;

    public TextField txtId;

    public TextField txtName;

    public void initialize() {
        btnDelete.setDisable(true);

        //set relevant columns
        tblStudent.getColumns().get(0).setCellValueFactory(new PropertyValueFactory<>("id"));
        tblStudent.getColumns().get(1).setCellValueFactory(new PropertyValueFactory<>("name"));
        tblStudent.getColumns().get(2).setCellValueFactory(new PropertyValueFactory<>("address"));
        tblStudent.getColumns().get(3).setCellValueFactory(new PropertyValueFactory<>("contact"));
        tblStudent.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); // allow multiple selections in table

        loadAllStudents();

        tblStudent.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> {
            btnDelete.setDisable(current == null);
            btnSave.setDisable(current != null);
            if (current == null) return; // if nothing is selected, return

        });

        tblStudent.getSelectionModel().getSelectedItems().addListener((ListChangeListener<? super Student>) change -> {
            if (tblStudent.getSelectionModel().getSelectedItems().size() > 1) { // if multiple rows are selected
                for (TextField txt : new TextField[]{txtId, txtName, txtAddress, txtContact}) {
                    txt.setText("MULTIPLE SELECTION - (%d) SELECTED".formatted(
                            tblStudent.getSelectionModel().getSelectedItems().size()
                    ));
                }
            } else if (tblStudent.getSelectionModel().getSelectedItems().size() == 1) { // if only one row is selected
                Student selectedStudent = tblStudent.getSelectionModel().getSelectedItem();
                txtId.setText(selectedStudent.getId());
                txtName.setText(selectedStudent.getName());
                txtAddress.setText(selectedStudent.getAddress());
                txtContact.setText(selectedStudent.getContact());
            } else { // if no row is selected
                for (TextField txt : new TextField[]{txtId, txtName, txtAddress, txtContact}) txt.clear();
                txtId.setText("GENERATED ID");
            }
        });
    }

    private boolean isValidName(String name) {
        String regExp = "[a-zA-Z ]{3,}";
        return name.strip().matches(regExp);
    }

    private boolean isValidAddress(String address) {
        String regExp = ".{3,}";
        return address.strip().matches(regExp);
    }

    private boolean isValidContact(String contact) {
        String regExp = "0\\d{2}-\\d{7}";
        return contact.strip().matches(regExp);
    }

    private boolean isValidData() {

        if (!isValidName(txtName.getText())) {
            txtName.requestFocus();
            txtName.selectAll();
            return false;
        }
        if (!isValidAddress(txtAddress.getText())) {
            txtAddress.requestFocus();
            txtAddress.selectAll();
            return false;
        }
        if (!txtContact.getText().isEmpty() && !isValidContact(txtContact.getText())) {
            txtContact.requestFocus();
            txtContact.selectAll();
            return false;

        }

        return true;
    }

    private String formatId(int id) {
        return "S-%03d".formatted(id);
    }

    private void generateNewId() {

    }

    private void deleteSelectedStudents() {
        ObservableList<Student> selectedStudents = tblStudent.getSelectionModel().getSelectedItems();
        try {
            Connection connection = SingletonConnection.getInstance().getConnection();
            String sql = """
                    DELETE FROM student where id=?
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            connection.setAutoCommit(false); //start transaction
            try {
                for (Student student : selectedStudents) {
                    preparedStatement.setInt(1, Integer.parseInt(student.getId().substring(2)));
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
                tblStudent.getItems().removeAll(selectedStudents); //remove deleted students from table
                btnNewStudent.fire();
            } catch (Throwable t) {
                connection.rollback(); // clear
                t.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Failed to delete student(s), try again").show();
            } finally {
                connection.setAutoCommit(true); //end transaction
            }
        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    private void loadAllStudents() {
        try {
            Connection connection = SingletonConnection.getInstance().getConnection();
            String sql = "TABLE student";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                Student student = new Student(formatId(resultSet.getInt("id")), resultSet.getString("name")
                        , resultSet.getString("address"), resultSet.getString("contact"));
                ObservableList<Student> studentList = tblStudent.getItems();
                studentList.add(student);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void btnDeleteOnAction(ActionEvent event) {
        deleteSelectedStudents();
    }

    public void btnNewStudentOnAction(ActionEvent event) {
        tblStudent.getSelectionModel().clearSelection();
        for (TextField textField : new TextField[]{txtName, txtAddress, txtContact}) {
            textField.clear();
        }
        txtId.setText("GENERATED ID");
        txtName.requestFocus();
    }

    public void btnSaveOnAction(ActionEvent event) {
        boolean validData = isValidData();

        if (!validData) return;

        String name = txtName.getText().strip();
        String address = txtAddress.getText().strip();
        String contact = txtContact.getText().strip();

        try {
            Connection connection = SingletonConnection.getInstance().getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    INSERT INTO student (name,address,contact) VALUES (?,?,?);
                    """, Statement.RETURN_GENERATED_KEYS);

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, address);
            preparedStatement.setString(3, contact.isEmpty() ? null : contact); //if contact is not given, set null

            int affectedRows = preparedStatement.executeUpdate();

            ResultSet generatedKeys = preparedStatement.getGeneratedKeys(); //get generated keys in server as a result set
            generatedKeys.next();
            int id = generatedKeys.getInt("id");

            ObservableList<Student> studentList = tblStudent.getItems();
            studentList.add(new Student(formatId(id), name, address, contact));
            btnNewStudent.fire();

        } catch (SQLException e) {
            //This is not a good practice at all
            //This should be handled as a business validation
            if (e.getSQLState().equals("23505")) {
                new Alert(Alert.AlertType.ERROR, "Contact number already exists").show();
                txtContact.requestFocus();
                txtContact.selectAll();
                return;
            }
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Something went wrong, try again!!").show();
        }


    }

    public void tblStudentOnKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) {
            btnDelete.fire();
        }
    }

    public void tblStudentOnKeyReleased(KeyEvent event) {

    }

}
