package src.model.system.course;

import java.util.List;
import src.model.person.Student;
import src.model.person.Teacher;

public class Course {
    private String courseId;
    private String courseName;
    private String subject;
    private CourseDate date;
    private Teacher teachers;
    private List<Student> students;
    private int totalCurrentStudent;
    private float process;

}
