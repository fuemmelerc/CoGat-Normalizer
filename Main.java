/*
 * Test Score Normalizer
 * Property of Caleb Fuemmeler
 * 9/16/2024
 */

import javax.swing.*;
import java.awt.event.*;
import javax.swing.filechooser.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.Period;
import java.lang.StringBuilder;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        // create user interface to select data file
        Thread uiThread = new Thread(new UI());
        uiThread.start();
        File studentData = UI.getData();
        while (studentData == null) {
            System.out.print("");
            studentData = UI.getData();
        }
        // Tell user they've picked the a file or an error otherwise
        UI.addInfo("got file " + studentData.getName());
        PrintWriter err = new PrintWriter("process_err.txt", "UTF-8");
        err.println(studentData.getAbsolutePath());
        try {
            // create tables for score processing
            List<String[]> rtu = parseCSV("Tables/raw_to_uss.csv");
            List<String[]> uts = parseCSV("Tables/uss_to_sas.csv");
            List<String[]> stp = parseCSV("Tables/sas_to_pr.csv");
            HashMap<String, List<String[]>> readingTests = new HashMap<String, List<String[]>>();
            /* These are all possible reading tests currently able to be processed (more in progress):
             * RSK
             * RW1
             * RS1
             * RF2
             * RW2
             * RS2
             * RF3
             * RW3
             * RS3
             * RF4
             * RW4
             * RS4
             * RF5
             * RW5
             * RS5
             * RF6
             * RW6
             * RS6
             * RF7
             * RW7
             * RS7
             * RF8
            */
            // Student -- Birthdate -- CoGat Test -- Test Date -- Raw Score (CoGat) -- Reading Test -- Raw Score (Reading)
            List<String[]> students = parseCSV(studentData.getAbsolutePath());
            PrintWriter writer = new PrintWriter("output file.csv", "UTF-8");
            writer.println("Student,Birthdate,CoGat Test,Test Date,Raw Score (CoGat),USS,SAS,PR,Reading Test,Raw Score (Reading), PR/Stanine");
            
            UI.addInfo("starting processing");
            for (int i = 1; i < students.size(); i++) {
                String[] line = students.get(i);
                String[] bdayString = line[1].split("/");
                String test = line[2];
                String[] testDateString = line[3].split("/");
                StringBuilder toBuild = new StringBuilder();
                toBuild.append(line[0]);
                toBuild.append(',');
                toBuild.append(line[1]);
                toBuild.append(',');
                toBuild.append(line[2]);
                toBuild.append(',');
                toBuild.append(line[3]);
                int raw = -1;
                toBuild.append(',');
                if (!line[4].equals("")) {
                    raw = Integer.parseInt(line[4]);
                    toBuild.append(line[4]);
                }
                int uss = -1;
                int sas = -1;
                int pr = -1;

                // Determine uss score from the raw score using the raw to uss table
                if (raw != -1) {
                    if (test.equals("5/6")) {
                        uss = Integer.parseInt(rtu.get(raw + 1)[1]);
                    } else if (test.equals("7")) {
                        uss = Integer.parseInt(rtu.get(raw + 1)[2]);
                    } else {
                        err.println("improper \"Test\" format for student " + line[0]);
                        UI.addInfo("test was improperly formatted for student " + line[0]);
                        return;
                    }

                    // Calculate the age of the student at the test date, required for determining sas score
                    // This is currently accepted year-month calculation, may be subject to change
                    int birthYear = Integer.parseInt(bdayString[2]);
                    int birthMonth = Integer.parseInt(bdayString[0]);
                    int testYear = Integer.parseInt(testDateString[2]);
                    int testMonth = Integer.parseInt(testDateString[0]);
                    int yearsBetween = 0;
                    int monthsBetween = 0;

                    if (birthMonth <= testMonth) {
                        yearsBetween = testYear - birthYear;
                        monthsBetween = testMonth - birthMonth;
                    } else {
                        yearsBetween = testYear - birthYear - 1;
                        monthsBetween = 12 - (birthMonth - testMonth);
                    }
                    double age = yearsBetween;

                    if (monthsBetween >= 2 && monthsBetween <= 4) {
                        age += 0.3;
                    } else if (monthsBetween >= 5 && monthsBetween <= 7) {
                        age += 0.6;
                    } else if (monthsBetween >= 8 && monthsBetween <= 10) {
                        age += 0.9;
                    } else if (monthsBetween >= 11) {
                        age += 1.0;
                    }

                    // These are two other possible calculations with slightly varying results
                    /*
                    LocalDate bday = LocalDate.of(Integer.parseInt(bdayString[2]), Integer.parseInt(bdayString[0]), Integer.parseInt(bdayString[1]));
                    LocalDate testDate = LocalDate.of(Integer.parseInt(testDateString[2]), Integer.parseInt(testDateString[0]), Integer.parseInt(testDateString[1]));
                    Period timeBetween = Period.between(bday, testDate);
                    System.out.println(timeBetween.toString());
                    double age = timeBetween.getYears();
                    int months = timeBetween.getMonths();
                    int days = timeBetween.getDays();
                    //uss to sas table requires this format for age
                     5.0 is 0-1, 5.3 is 2-4, 5.6 is 5-7, 5.9 is 8-10, 6.0 is 11-1
                     1 month 16 days = 5.3, 4 months 16 days = 5.6, 7 months 16 days = 5.9, 10 months 16 days = 6.0
                    */

                    /*
                    if (months == 1 && days > 15) {
                        age += 0.3;
                    } else if (months >= 2 && months <= 3) {
                        age += 0.3;
                    } else if (months == 4) {
                        if (days <= 15) {
                            age += 0.3;
                        } else {
                            age += 0.6;
                        }
                    } else if (months >= 5 && months <= 6) {
                        age += 0.6;
                    } else if (months == 7) {
                        if (days <= 15) {
                            age += 0.6;
                        } else {
                            age += 0.9;
                        }
                    } else if (months >= 8 && months <= 9) {
                        age += 0.9;
                    } else if (months == 10) {
                        if (days <= 15) {
                            age += 0.9;
                        } else {
                            age += 1.0;
                        }
                    } else if (months >= 11) {
                        age += 1.0;
                    }
                    */

                    /*
                    if (months >= 2 && months <= 4) {
                        age += 0.3;
                    } else if (months >= 5 && months <= 7) {
                        age += 0.6;
                    } else if (months >= 8 && months <= 10) {
                        age += 0.9;
                    } else if (months >= 11) {
                        age += 1.0;
                    }
                    */
                    String[] utsHeader = uts.get(0);
                    // Determine correct column to look in for uss score
                    int column = 0;
                    for (int col = 1; col < utsHeader.length; col++) {
                        if (age == Double.parseDouble(utsHeader[col])) {
                            column = col;
                            break;
                        }
                    }
                    if (column == 0) {
                        err.println("couldn't find age for student " + line[0]);
                        UI.addInfo("couldn't find age for student " + line[0]);
                        return;
                    }
                
                    // Look through column for the uss score, the row is the sas score
                    for (int row = 1; row < uts.size(); row++) {
                        String ussCheck = uts.get(row)[column];
                        if (ussCheck.equals("")) {
                            continue;
                        } else if (ussCheck.contains("-")) {
                            String[] bounds = ussCheck.split("-");
                            if (uss >= Integer.parseInt(bounds[0]) && uss <= Integer.parseInt(bounds[1])) {
                                sas = Integer.parseInt(uts.get(row)[0]);
                                break;
                            }
                        } else if (uss == Integer.parseInt(ussCheck)) {
                            sas = Integer.parseInt(uts.get(row)[0]);
                            break;
                        }
                    }
                    if (sas == -1) {
                        err.println("couldn't find sas for student " + line[0]);
                        UI.addInfo("couldn't find sas for student " + line[0]);
                        return;
                    }

                    // Use the sas score to determine the pr score
                    for (int row = 1; row < stp.size(); row++) {
                        String sasCheck = stp.get(row)[0];
                        if (sasCheck.contains("-")) {
                            String[] bounds = sasCheck.split("-");
                            if (sas >= Integer.parseInt(bounds[0]) && sas <= Integer.parseInt(bounds[1])) {
                                pr = Integer.parseInt(stp.get(row)[1]);
                                break;
                            }
                        } else if (sas == Integer.parseInt(sasCheck)) {
                            pr = Integer.parseInt(stp.get(row)[1]);
                            break;
                        }
                    }
                    if (pr == -1) {
                        err.println("couldn't find pr for student " + line[0]);
                        UI.addInfo("couldn't find pr for student " + line[0]);
                        return;
                    }
                }
                toBuild.append(',');
                if (uss != -1) {
                    toBuild.append(uss);
                }
                toBuild.append(',');
                if (sas != -1) {
                    toBuild.append(sas);
                }
                toBuild.append(',');
                if (pr != -1) {
                    toBuild.append(pr);
                }
                String rTest = line[5];
                toBuild.append(',');
                toBuild.append(line[5]);
                int rRaw = -1;
                toBuild.append(',');
                if (!line[6].equals("")) {
                    rRaw = Integer.parseInt(line[6]);
                    toBuild.append(line[6]);
                }
                int rpr = -1;
                if (rRaw != -1) {
                    if (readingTests.containsKey(rTest)) {
                        rpr = Integer.parseInt(readingTests.get(rTest).get(rRaw + 1)[1]);
                    } else {
                        readingTests.put(rTest, parseCSV("Tables/" + rTest + ".csv"));
                        rpr = Integer.parseInt(readingTests.get(rTest).get(rRaw + 1)[1]);
                    }
                }
                toBuild.append(',');
                if (rpr != -1) {
                    toBuild.append(rpr);
                }
                writer.println(toBuild.toString());
            }
            writer.close();
            UI.addInfo("Finished processing, files should be there");
        } catch (Exception e) {
            err.println(e.getMessage());
            e.printStackTrace();
            UI.addInfo(e.getMessage());
        }
        err.close();
    }

    public static List<String[]> parseCSV(String fileName) throws FileNotFoundException, IOException{
        List<String[]> records = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        while((line = br.readLine()) != null) {
            for (int i = 1; i < line.length(); i++) {
                if (line.charAt(i) == ',' && line.charAt(i - 1) == ',') {
                    line = line.substring(0, i) + " " + line.substring(i);
                }
                if (line.charAt(i) == ',' && i == line.length() - 1) {
                    line = line + " ";
                }
            }
            String[] values = line.split(",");
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(" ")) {
                    values[i] = "";
                }
            }
            records.add(values);
        }
        return records;
    }
}

class UI extends JFrame implements ActionListener, Runnable {
    private static JFrame f;
    private JButton b;
    private JFileChooser jf;
    private static File studentData;
    private static ArrayList<JLabel> info;

    public void run() {
        f = new JFrame();
        b = new JButton("Choose File");
        //setBounds(int xaxis, int yaxis, int width, int height)
        b.setBounds(70, 100, 200, 40);
        b.addActionListener(this);
        f.add(b);
        f.setSize(400, 500);
        JLabel title = new JLabel("Normalizer");
        title.setBounds(0, 0, 200, 40);
        f.add(title);
        f.setLayout(null);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        info = new ArrayList<JLabel>();
        jf = new JFileChooser();
    }

    public void actionPerformed(ActionEvent e) {
        try {
            if (e.getSource() == b) {
                int returnVal = jf.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    studentData = jf.getSelectedFile();

                } else {
                    //System.out.println("Didn't open a file");
                }
            }
        } catch (Exception ex) {
            //System.out.println("error");
        }
    }

    public static void addInfo(String text) {
        info.add(new JLabel(text, JLabel.CENTER));
        info.get(info.size() - 1).setVerticalTextPosition(JLabel.BOTTOM);
        info.get(info.size() - 1).setHorizontalTextPosition(JLabel.CENTER);
        info.get(info.size() - 1).setBounds(20, 150, 300, 40);
        if (info.size() - 1 > 0) {
            info.get(info.size() - 1).setBounds(20, info.get(info.size() - 2).getY() + 40, 300, 40);
        }
        info.get(info.size() - 1).setOpaque(true);
        f.add(info.get(info.size() - 1));
        f.revalidate();
        f.repaint();
    }

    public static File getData() {
        return studentData;
    }
}