package csssingleliner;

import javax.swing.*;
import javax.swing.text.*;

import java.awt.*;
import java.awt.event.ItemEvent; // ItemListener 사용
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern; // Pattern 클래스 임포트

public class CSSSingleLiner extends JFrame {

    private JTextPane outputTextPane;
    private JPanel dropPanel;
    private JCheckBox removeCommentsCheckbox; // 주석 제거 체크박스 추가

    // 스타일 정의 (이전과 동일)
    private Style styleDefault, styleComment, styleSelector, styleProperty, styleValue, styleBrace, styleAtRule, stylePunctuation;

    // 원본 처리 결과 저장 변수 (주석 포함, 한 줄 변환 적용된 상태)
    private String processedCssWithComments = null;
    private String currentFileName = null; // 현재 처리된 파일 이름 저장

    public CSSSingleLiner() {
        super("CSSSingleLiner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        

        // --- 프레임 아이콘 설정 ---
        try {
            URL iconURL = getClass().getResource("icon.png");

            if (iconURL != null) {
                ImageIcon frameIcon = new ImageIcon(iconURL);
                // JFrame의 아이콘으로 설정
                setIconImage(frameIcon.getImage());
            } else {
                // 아이콘 파일을 찾지 못한 경우 에러 메시지 출력 (콘솔)
                System.err.println("아이콘 리소스를 찾을 수 없습니다: /icon.png");
            }
        } catch (Exception e) {
            // 아이콘 로딩 중 예외 발생 시 에러 메시지 출력
             System.err.println("아이콘 로딩 중 오류 발생: " + e.getMessage());
             e.printStackTrace();
        }

        // --- UI 구성 ---
        outputTextPane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                // 이 메서드가 false를 반환하면, JTextPane은 JScrollPane의 뷰포트 너비에
                // 맞춰 내용을 줄바꿈하지 않고, 내용 자체의 너비를 기준으로 크기를 결정합니다.
                // 따라서 내용이 길어지면 가로 스크롤바가 나타나게 됩니다.
                return false;
            }
        };
        outputTextPane.setEditable(false); // 편집 불가능 설정
        initializeStyles(); // JTextPane 객체 생성 후 스타일 초기화

        // JScrollPane 생성 및 가로 스크롤바 정책 설정
        JScrollPane scrollPane = new JScrollPane(outputTextPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); // 가로 스크롤바 추가

        // 드롭 패널 설정 (BorderLayout 사용)
        dropPanel = new JPanel(new BorderLayout(5, 5)); // 여백 추가
        dropPanel.setBackground(Color.LIGHT_GRAY);
        dropPanel.setPreferredSize(new Dimension(250, 600)); // 너비 조정

        // 드롭 안내 레이블
        JLabel dropLabel = new JLabel("<html><center>여기에 CSS 파일을<br>드롭하세요</center></html>", SwingConstants.CENTER); // 멀티라인 HTML 사용
        dropLabel.setForeground(Color.DARK_GRAY);
        dropPanel.add(dropLabel, BorderLayout.CENTER);

        // 컨트롤 패널 (체크박스 등 추가)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(Color.LIGHT_GRAY); // 배경색 일치
        removeCommentsCheckbox = new JCheckBox("주석 제거");
        removeCommentsCheckbox.setBackground(Color.LIGHT_GRAY); // 배경색 일치
        controlPanel.add(removeCommentsCheckbox);
        dropPanel.add(controlPanel, BorderLayout.NORTH); // 드롭 패널 상단에 추가

        // 체크박스 리스너 추가 (ItemListener 사용 권장)
        removeCommentsCheckbox.addItemListener(e -> {
             // 체크박스 상태 변경 시 화면 업데이트
             if (processedCssWithComments != null) {
                 updateOutputPane(); // 저장된 내용을 기반으로 화면 갱신
             }
         });


        dropPanel.setTransferHandler(new FileDropHandler(this));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, dropPanel, scrollPane);
        splitPane.setDividerLocation(250); // 분할선 위치 고정

        getContentPane().add(splitPane);
    }

    // 스타일 초기화 메서드 (이전과 동일)
    private void initializeStyles() {
        StyledDocument doc = outputTextPane.getStyledDocument();
        styleDefault = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(styleDefault, "Monospaced");
        StyleConstants.setFontSize(styleDefault, 12);
        StyleConstants.setForeground(styleDefault, Color.BLACK);
        styleComment = doc.addStyle("Comment", styleDefault);
        StyleConstants.setForeground(styleComment, Color.GRAY);
        StyleConstants.setItalic(styleComment, true);
        styleSelector = doc.addStyle("Selector", styleDefault);
        StyleConstants.setForeground(styleSelector, new Color(0, 0, 139));
        StyleConstants.setBold(styleSelector, true);
        styleProperty = doc.addStyle("Property", styleDefault);
        StyleConstants.setForeground(styleProperty, new Color(139, 0, 0));
        styleValue = doc.addStyle("Value", styleDefault);
        StyleConstants.setForeground(styleValue, new Color(0, 128, 0));
        styleBrace = doc.addStyle("Brace", styleDefault);
        StyleConstants.setForeground(styleBrace, Color.ORANGE.darker());
        StyleConstants.setBold(styleBrace, true);
        styleAtRule = doc.addStyle("AtRule", styleDefault);
        StyleConstants.setForeground(styleAtRule, new Color(180, 0, 180));
        StyleConstants.setBold(styleAtRule, true);
        stylePunctuation = doc.addStyle("Punctuation", styleDefault);
        StyleConstants.setForeground(stylePunctuation, Color.DARK_GRAY);
    }

    // 파일을 읽고 처리하는 메서드
    public void processFile(File file) {
        if (file != null && file.getName().toLowerCase().endsWith(".css")) {
            try {
                String originalCssContent = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                currentFileName = file.getName(); // 파일 이름 저장

                // 1. 한 줄 변환 처리 (주석 제거 로직은 여기서 제외)
                processedCssWithComments = processCssContent(originalCssContent);

                // 2. 화면 업데이트 (체크박스 상태 및 개행 처리 포함)
                updateOutputPane();

                // 드롭 패널 레이블 업데이트
                updateDropLabel(currentFileName + " 로드됨");

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "파일을 읽는 중 오류 발생:\n" + e.getMessage(), "파일 읽기 오류", JOptionPane.ERROR_MESSAGE);
                displayError("파일 읽기 오류: " + file.getName());
                processedCssWithComments = null; // 오류 발생 시 저장된 내용 초기화
                currentFileName = null;
                updateDropLabel("오류 발생");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "CSS 처리 중 오류 발생:\n" + e.getMessage(), "처리 오류", JOptionPane.ERROR_MESSAGE);
                 displayError("CSS 처리 오류: " + file.getName() + "\n" + e.toString());
                 processedCssWithComments = null;
                 currentFileName = null;
                 updateDropLabel("오류 발생");
                 e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(this, "유효한 CSS 파일(.css)을 드롭해주세요.", "잘못된 파일", JOptionPane.WARNING_MESSAGE);
             displayError("잘못된 파일 형식입니다.");
             processedCssWithComments = null;
             currentFileName = null;
             updateDropLabel("잘못된 파일");
        }
    }

    // 드롭 패널 레이블 업데이트 헬퍼 메서드
    private void updateDropLabel(String text) {
         Component[] components = dropPanel.getComponents();
         // controlPanel 제외하고 JLabel 찾기
         for (Component comp : components) {
             if (comp instanceof JLabel) {
                 ((JLabel) comp).setText("<html><center>" + text.replace("\n", "<br>") + "</center></html>");
                 break; // 첫 번째 JLabel만 업데이트
             }
         }
    }


    // 저장된 내용을 기반으로 화면을 업데이트하는 메서드
    private void updateOutputPane() {
        if (processedCssWithComments == null) {
             displayError("먼저 CSS 파일을 로드하세요.");
             return;
        }

        String cssToStyle = processedCssWithComments;

        // 1. 주석 제거 (체크박스 확인)
        if (removeCommentsCheckbox.isSelected()) {
            // Pattern.DOTALL 플래그는 여러 줄에 걸친 주석(/* ... */)도 제거하도록 함
            cssToStyle = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(cssToStyle).replaceAll("");
        }

        // 2. 연속 개행 처리 (3개 이상 -> 2개)
        // \\R은 유니코드 줄바꿈 문자( \n, \r\n, \r 등)를 의미함
        cssToStyle = cssToStyle.replaceAll("(\\R\\s*){3,}", "\n\n"); // 결과는 항상 \n\n 으로 통일

        // 3. 스타일 적용
        applyStyles(cssToStyle);
        outputTextPane.setCaretPosition(0); // 스크롤 맨 위로

        // 파일 이름과 상태를 드롭 레이블에 다시 표시 (옵션)
        String status = removeCommentsCheckbox.isSelected() ? "(주석 제거됨)" : "(주석 포함)";
        if (currentFileName != null) {
            updateDropLabel(currentFileName + "\n" + status);
        }
    }


    // 오류 메시지를 JTextPane에 표시하는 메서드 (이전과 동일)
    private void displayError(String message) {
        StyledDocument doc = outputTextPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            Style errorStyle = doc.addStyle("Error", styleDefault);
            StyleConstants.setForeground(errorStyle, Color.RED);
            doc.insertString(0, message, errorStyle);
        } catch (BadLocationException e) { e.printStackTrace(); }
    }

 // CSS 내용을 한 줄로 변환하는 로직 (재귀 호출 추가)
    private String processCssContent(String cssContent) {
        StringBuilder result = new StringBuilder();
        StringBuilder currentBlockLines = null; // 현재 블록 라인 저장
        boolean inBlock = false;

        String[] lines = cssContent.split("\\R");
        if (lines.length == 0 && cssContent.length() > 0) {
             // split이 안되는 한 줄 짜리 내용일 수 있음 (재귀 호출 시)
             lines = new String[]{ cssContent };
        } else if (lines.length == 0) {
            return ""; // 빈 내용 처리
        }


        for (String line : lines) {
            String trimmedLine = line.trim();

            if (inBlock) {
                // --- 블록 내부에 있을 때 ---
                currentBlockLines.append(line).append("\n");
                int openBraceCount = countOccurrences(currentBlockLines.toString(), '{');
                int closeBraceCount = countOccurrences(currentBlockLines.toString(), '}');

                // 블록이 완전히 닫혔는지 확인 (괄호 짝이 맞는지)
                if (openBraceCount > 0 && openBraceCount == closeBraceCount) {
                    // --- 블록 처리 ---
                    String blockContent = currentBlockLines.toString();
                    int openingBraceIndex = blockContent.indexOf('{');
                    // 마지막 닫는 괄호를 찾아야 정확한 내용 추출 가능
                    int closingBraceIndex = blockContent.lastIndexOf('}');

                    if (openingBraceIndex != -1 && closingBraceIndex > openingBraceIndex) {
                        String selectorPart = blockContent.substring(0, openingBraceIndex).trim();
                        // 중첩 구조를 고려하여 첫 '{' 와 마지막 '}' 사이의 내용을 추출
                        String contentPart = blockContent.substring(openingBraceIndex + 1, closingBraceIndex);

                        // @media 또는 @feature 블록인지 확인
                        if (selectorPart.startsWith("@media") || selectorPart.startsWith("@feature")) {
                            // @media/@feature 블록 처리
                            result.append(selectorPart).append(" {\n"); // @media 선언부 추가
                            // 내부 내용을 재귀적으로 처리
                            String processedInnerContent = processCssContent(contentPart.trim()); // 내부 공백 제거 후 재귀 호출
                            // 재귀 처리된 내용을 라인별로 추가 (들여쓰기 옵션)
                            for(String innerLine : processedInnerContent.split("\\R")) {
                                if (!innerLine.trim().isEmpty()) {
                                    // result.append("  ").append(innerLine).append("\n"); // 들여쓰기 추가 시
                                    result.append(innerLine).append("\n"); // 들여쓰기 없이 추가
                                }
                            }
                            result.append("}\n"); // @media 닫는 괄호 추가
                        } else {
                            // 일반 CSS 규칙 블록 처리 (기존 로직)
                            String processedContent = contentPart
                                    .replaceAll("\\s*\\R\\s*", " ") // 줄바꿈과 주변 공백 -> 단일 공백
                                    .replaceAll("\\s{2,}", " ")   // 연속된 공백 -> 단일 공백
                                    .replaceAll("\\s*([{};:])\\s*", "$1") // 특수문자 주변 공백 제거
                                    .trim();

                            // 내용이 비어있지 않은 경우에만 추가 (주석만 있던 블록 등 제외)
                            if (!processedContent.isEmpty()) {
                                 result.append(selectorPart).append(" { ").append(processedContent).append(" }\n");
                            } else if (!selectorPart.isEmpty()) {
                                // 내용이 없어도 선택자는 유지 (예: a { } )
                                result.append(selectorPart).append(" { }\n");
                            }
                        }
                    } else {
                        // 괄호 매칭 실패 등 비정상 구조 시 원본 추가
                        result.append(currentBlockLines);
                    }
                    // 상태 초기화
                    inBlock = false;
                    currentBlockLines = null;
                }
                // else: 아직 블록 내부 (중첩된 경우), 계속 라인 추가
            } else {
                // --- 블록 외부에 있을 때 ---
                if (trimmedLine.contains("{")) {
                    // --- 블록 시작 감지 ---
                    inBlock = true;
                    currentBlockLines = new StringBuilder();
                    currentBlockLines.append(line).append("\n");

                    // 한 줄에 블록 시작과 끝이 모두 있는지 확인 (재귀 호출 시 중요)
                    int openBraceCount = countOccurrences(trimmedLine, '{');
                    int closeBraceCount = countOccurrences(trimmedLine, '}');
                    if (openBraceCount > 0 && openBraceCount == closeBraceCount) {
                         // --- 한 줄 블록 즉시 처리 ---
                         String blockContent = currentBlockLines.toString(); // 현재 라인만 포함됨
                         int openingBraceIndex = blockContent.indexOf('{');
                         int closingBraceIndex = blockContent.lastIndexOf('}'); // 같은 라인에 있음

                         if (openingBraceIndex != -1 && closingBraceIndex > openingBraceIndex) {
                             String selectorPart = blockContent.substring(0, openingBraceIndex).trim();
                             String contentPart = blockContent.substring(openingBraceIndex + 1, closingBraceIndex);

                             if (selectorPart.startsWith("@media") || selectorPart.startsWith("@feature")) {
                                 // 한 줄짜리 @media/@feature 블록 처리
                                 result.append(selectorPart).append(" {\n");
                                 String processedInnerContent = processCssContent(contentPart.trim());
                                 for(String innerLine : processedInnerContent.split("\\R")) {
                                     if (!innerLine.trim().isEmpty()) {
                                         // result.append("  ").append(innerLine).append("\n"); // Optional indent
                                         result.append(innerLine).append("\n");
                                     }
                                 }
                                 result.append("}\n");
                             } else {
                                  // 한 줄짜리 일반 블록 처리
                                  String processedContent = contentPart
                                          .replaceAll("\\s*\\R\\s*", " ") // 의미는 적지만 포함
                                          .replaceAll("\\s{2,}", " ")
                                          .replaceAll("\\s*([{};:])\\s*", "$1")
                                          .trim();
                                  if (!processedContent.isEmpty()) {
                                       result.append(selectorPart).append(" { ").append(processedContent).append(" }\n");
                                  } else if (!selectorPart.isEmpty()){
                                       result.append(selectorPart).append(" { }\n");
                                  }
                             }
                         } else {
                              result.append(currentBlockLines); // 비정상
                         }
                         // 상태 즉시 초기화
                         inBlock = false;
                         currentBlockLines = null;
                    }
                    // else: 여러 줄 블록 시작, 다음 라인에서 계속 처리
                } else {
                    // 블록 외부의 다른 라인 (최상위 주석, @import 등)
                    // 비어있지 않은 라인만 추가 (불필요한 공백 라인 제거 효과)
                    if (!trimmedLine.isEmpty()) {
                         result.append(line).append("\n");
                    }
                }
            }
        }

        // 파일 끝까지 처리했는데 블록이 닫히지 않은 경우 대비
        if (currentBlockLines != null) {
            result.append(currentBlockLines.toString());
        }

        // 최종 결과 반환 (앞뒤 공백 제거 옵션)
        return result.toString(); //.trim(); // trim() 제거하여 앞뒤 공백 유지
    }

    // 문자열 내 특정 문자 개수 세는 헬퍼 메서드 (이전과 동일)
    private int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (char c : haystack.toCharArray()) { if (c == needle) count++; }
        return count;
    }

    // Syntax Highlighting Logic (이전과 거의 동일)
    private void applyStyles(String cssContent) {
        StyledDocument doc = outputTextPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            StringBuilder currentToken = new StringBuilder();
            char state = ' ';
            for (int i = 0; i < cssContent.length(); i++) {
                char c = cssContent.charAt(i);
                char nextChar = (i + 1 < cssContent.length()) ? cssContent.charAt(i + 1) : '\0';
                // ... (상태 머신 로직은 이전과 동일하게 유지) ...
                 switch (state) {
                    case ' ': // 기본 상태 (블록 외부 또는 블록 끝난 직후)
                        if (c == '/' && nextChar == '*') {
                            insertToken(doc, currentToken, styleDefault); // 이전 토큰 삽입
                            currentToken.append(c); // '/' 추가
                            state = '/';
                        } else if (c == '@') {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c);
                            state = '@';
                        } else if (c == '{') {
                            insertToken(doc, currentToken, styleSelector);
                            currentToken.append(c);
                            insertToken(doc, currentToken, styleBrace);
                            state = '{';
                        } else if (c == '}') {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c);
                            insertToken(doc, currentToken, styleBrace);
                        } else if (!Character.isWhitespace(c)) {
                            if (currentToken.length() == 0) state = 's';
                            currentToken.append(c);
                        } else {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c);
                            insertToken(doc, currentToken, styleDefault);
                        }
                        break;
                    case '/':
                        currentToken.append(c);
                        if (c == '*') { state = '*'; }
                        else {
                            insertToken(doc, currentToken, styleDefault);
                            state = ' '; i--;
                        }
                        break;
                    case '*':
                        currentToken.append(c);
                        if (c == '*' && nextChar == '/') {
                            currentToken.append(nextChar);
                            insertToken(doc, currentToken, styleComment); // 주석 스타일 적용!
                            i++; state = ' ';
                        }
                        break;
                    case '@':
                        if (Character.isLetterOrDigit(c) || c == '-') { currentToken.append(c); }
                        else {
                            insertToken(doc, currentToken, styleAtRule);
                            state = ' '; i--;
                        }
                        break;
                    case 's':
                        if (c == '{') {
                            insertToken(doc, currentToken, styleSelector);
                            currentToken.append(c);
                            insertToken(doc, currentToken, styleBrace);
                            state = '{';
                        } else if (c == '/' && nextChar == '*') {
                            insertToken(doc, currentToken, styleSelector);
                            currentToken.append(c); state = '/';
                        } else { currentToken.append(c); }
                        break;
                    case '{':
                        if (c == '}') {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c);
                            insertToken(doc, currentToken, styleBrace);
                            state = ' ';
                        } else if (c == '/' && nextChar == '*') {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c); state = '/';
                        } else if (!Character.isWhitespace(c)) {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c); state = 'p';
                        } else {
                             insertToken(doc, currentToken, styleDefault);
                             currentToken.append(c);
                             insertToken(doc, currentToken, styleDefault);
                        }
                        break;
                    case 'p':
                         if (c == ':') {
                            insertToken(doc, currentToken, styleProperty);
                            currentToken.append(c);
                            insertToken(doc, currentToken, stylePunctuation);
                            state = ':';
                        } else if (c == '/' && nextChar == '*') {
                             insertToken(doc, currentToken, styleProperty);
                             currentToken.append(c); state = '/';
                        } else { currentToken.append(c); }
                        break;
                    case ':':
                        if (c == '/' && nextChar == '*') {
                             insertToken(doc, currentToken, styleDefault);
                             currentToken.append(c); state = '/';
                        } else if (!Character.isWhitespace(c)) {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c); state = 'v';
                        } else {
                            insertToken(doc, currentToken, styleDefault);
                            currentToken.append(c);
                            insertToken(doc, currentToken, styleDefault);
                        }
                        break;
                    case 'v':
                        if (c == ';') {
                            insertToken(doc, currentToken, styleValue);
                            currentToken.append(c);
                            insertToken(doc, currentToken, stylePunctuation);
                            state = '{';
                        } else if (c == '}') {
                            insertToken(doc, currentToken, styleValue);
                            currentToken.append(c);
                            insertToken(doc, currentToken, styleBrace);
                            state = ' ';
                        } else if (c == '/' && nextChar == '*') {
                            insertToken(doc, currentToken, styleValue);
                            currentToken.append(c); state = '/';
                        } else { currentToken.append(c); }
                        break;
                }
            }
             if (currentToken.length() > 0) {
                Style finalStyle = styleDefault;
                 if(state == 's') finalStyle = styleSelector;
                 else if (state == 'p') finalStyle = styleProperty;
                 else if (state == 'v') finalStyle = styleValue;
                 else if (state == '*') finalStyle = styleComment;
                 else if (state == '@') finalStyle = styleAtRule;
                insertToken(doc, currentToken, finalStyle);
            }
        } catch (BadLocationException e) {
             e.printStackTrace();
             displayError("스타일 적용 중 오류 발생:\n" + e.getMessage());
        }
    }

    // 토큰 삽입 헬퍼 메서드 (이전과 동일)
    private void insertToken(StyledDocument doc, StringBuilder token, Style style) throws BadLocationException {
        if (token.length() > 0) {
            // 주석 제거가 체크되었고 현재 스타일이 주석 스타일이면 삽입하지 않음
            // -> 이 로직은 applyStyles 호출 전에 문자열에서 제거하는 방식으로 변경됨. 여기서는 항상 삽입.
            doc.insertString(doc.getLength(), token.toString(), style);
            token.setLength(0);
        }
    }

    // --- 애플리케이션 실행 ---
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel.");
        }
        SwingUtilities.invokeLater(() -> {
            CSSSingleLiner frame = new CSSSingleLiner();
            frame.setVisible(true);
        });
    }
}