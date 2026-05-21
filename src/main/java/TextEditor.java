import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TextEditor {
    private final List<StringBuilder> lines = new ArrayList<>();
    private final Stack<EditorCommand> undoStack = new Stack<>();
    private final Stack<EditorCommand> redoStack = new Stack<>();
    private int row = 0;
    private int col = 0;
    private Mode mode = Mode.NORMAL;

    private interface EditorCommand {
        void undo(TextEditor editor);

        void redo(TextEditor editor);
    }

    public TextEditor() {
        lines.add(new StringBuilder());
    }

    public void insertChar(char c) {
        requireInsertMode();

        InsertCommand command = new InsertCommand(c, row, col);
        command.redo(this);
        recordCommand(command);
    }

    public void deleteChar() {
        requireInsertMode();

        if (col > 0) {
            DeleteCommand command = DeleteCommand.forCharacter(row, col, lines.get(row).charAt(col - 1));
            command.redo(this);
            recordCommand(command);
            return;
        }

        if (row > 0) {
            DeleteCommand command = DeleteCommand.forLineMerge(row, lines.get(row - 1).length(), lines.get(row).toString());
            command.redo(this);
            recordCommand(command);
        }
    }

    public void enterInsertMode() {
        mode = Mode.INSERT;
    }

    public void enterNormalMode() {
        mode = Mode.NORMAL;
    }

    public void moveToLineStart() {
        col = 0;
    }

    public void moveToLineEnd() {
        col = lines.get(row).length();
    }

    public void moveWordForward() {
        String text = getText();
        int index = toTextIndex(row, col);
        int nextWordStart = findNextWordStart(text, index);
        if (nextWordStart >= 0) {
            moveCursorToTextIndex(nextWordStart);
            return;
        }

        int wordEnd = findCurrentWordEnd(text, index);
        if (wordEnd >= 0) {
            moveCursorToTextIndex(wordEnd);
        }
    }

    public void moveWordBackward() {
        String text = getText();
        int index = toTextIndex(row, col);
        int previousWordStart = findPreviousWordStart(text, index);
        if (previousWordStart >= 0) {
            moveCursorToTextIndex(previousWordStart);
        }
    }

    public CursorPosition search(String target) {
        validateTarget(target);

        String text = getText();
        int matchIndex = text.indexOf(target, toTextIndex(row, col));
        if (matchIndex < 0) {
            return null;
        }

        moveCursorToTextIndex(matchIndex);
        return getCursor();
    }

    public int replaceAll(String target, String replacement) {
        validateTarget(target);
        if (replacement == null) {
            throw new IllegalArgumentException("replacement must not be null");
        }
        if (target.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("target must not contain newline");
        }
        if (replacement.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("replacement must not contain newline");
        }

        int total = 0;
        List<LineChange> changes = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String original = lines.get(i).toString();
            StringBuilder replaced = new StringBuilder();

            int start = 0;
            int match;
            while ((match = original.indexOf(target, start)) >= 0) {
                replaced.append(original, start, match);
                replaced.append(replacement);
                total++;
                start = match + target.length();
            }

            replaced.append(original.substring(start));
            String replacementLine = replaced.toString();
            if (!original.equals(replacementLine)) {
                changes.add(new LineChange(i, original, replacementLine));
            }
        }

        if (!changes.isEmpty()) {
            ReplaceAllCommand command = new ReplaceAllCommand(changes, row, col,
                    row, Math.min(col, replacementLengthForCurrentLine(changes, row)));
            command.redo(this);
            recordCommand(command);
        }
        return total;
    }

    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }

        EditorCommand command = undoStack.pop();
        command.undo(this);
        redoStack.push(command);
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }

        EditorCommand command = redoStack.pop();
        command.redo(this);
        undoStack.push(command);
    }

    public void moveLeft() {
        if (col > 0) {
            col--;
            return;
        }

        if (row > 0) {
            row--;
            col = lines.get(row).length();
        }
    }

    public void moveRight() {
        if (col < lines.get(row).length()) {
            col++;
            return;
        }

        if (row < lines.size() - 1) {
            row++;
            col = 0;
        }
    }

    public void moveUp() {
        if (row == 0) {
            return;
        }

        row--;
        col = Math.min(col, lines.get(row).length());
    }

    public void moveDown() {
        if (row == lines.size() - 1) {
            return;
        }

        row++;
        col = Math.min(col, lines.get(row).length());
    }

    public String getText() {
        return String.join("\n", lines.stream()
                .map(StringBuilder::toString)
                .toList());
    }

    public CursorPosition getCursor() {
        return new CursorPosition(row, col);
    }

    public Mode getMode() {
        return mode;
    }

    private void recordCommand(EditorCommand command) {
        undoStack.push(command);
        redoStack.clear();
    }

    private int replacementLengthForCurrentLine(List<LineChange> changes, int cursorRow) {
        for (LineChange change : changes) {
            if (change.row() == cursorRow) {
                return change.after().length();
            }
        }
        return lines.get(cursorRow).length();
    }

    private void setCursor(int cursorRow, int cursorCol) {
        row = cursorRow;
        col = cursorCol;
    }

    private record InsertCommand(char c, int beforeRow, int beforeCol) implements EditorCommand {
        @Override
        public void undo(TextEditor editor) {
            if (c == '\n') {
                StringBuilder nextLine = editor.lines.remove(beforeRow + 1);
                editor.lines.get(beforeRow).append(nextLine);
            } else {
                editor.lines.get(beforeRow).deleteCharAt(beforeCol);
            }
            editor.setCursor(beforeRow, beforeCol);
        }

        @Override
        public void redo(TextEditor editor) {
            if (c == '\n') {
                StringBuilder current = editor.lines.get(beforeRow);
                StringBuilder next = new StringBuilder(current.substring(beforeCol));
                current.delete(beforeCol, current.length());
                editor.lines.add(beforeRow + 1, next);
                editor.setCursor(beforeRow + 1, 0);
                return;
            }

            editor.lines.get(beforeRow).insert(beforeCol, c);
            editor.setCursor(beforeRow, beforeCol + 1);
        }
    }

    private record DeleteCommand(int beforeRow, int beforeCol, Character deletedChar,
                                 int previousLineLength, String removedLine) implements EditorCommand {
        static DeleteCommand forCharacter(int beforeRow, int beforeCol, char deletedChar) {
            return new DeleteCommand(beforeRow, beforeCol, deletedChar, -1, null);
        }

        static DeleteCommand forLineMerge(int beforeRow, int previousLineLength, String removedLine) {
            return new DeleteCommand(beforeRow, 0, null, previousLineLength, removedLine);
        }

        @Override
        public void undo(TextEditor editor) {
            if (deletedChar != null) {
                int insertCol = beforeCol - 1;
                editor.lines.get(beforeRow).insert(insertCol, deletedChar.charValue());
                editor.setCursor(beforeRow, beforeCol);
                return;
            }

            StringBuilder previousLine = editor.lines.get(beforeRow - 1);
            previousLine.delete(previousLineLength, previousLine.length());
            editor.lines.add(beforeRow, new StringBuilder(removedLine));
            editor.setCursor(beforeRow, beforeCol);
        }

        @Override
        public void redo(TextEditor editor) {
            if (deletedChar != null) {
                editor.lines.get(beforeRow).deleteCharAt(beforeCol - 1);
                editor.setCursor(beforeRow, beforeCol - 1);
                return;
            }

            StringBuilder current = editor.lines.remove(beforeRow);
            editor.lines.get(beforeRow - 1).append(current);
            editor.setCursor(beforeRow - 1, previousLineLength);
        }
    }

    private record LineChange(int row, String before, String after) {
    }

    private record ReplaceAllCommand(List<LineChange> changes, int beforeRow, int beforeCol,
                                     int afterRow, int afterCol) implements EditorCommand {
        private ReplaceAllCommand {
            changes = List.copyOf(changes);
        }

        @Override
        public void undo(TextEditor editor) {
            for (LineChange change : changes) {
                editor.lines.set(change.row(), new StringBuilder(change.before()));
            }
            editor.setCursor(beforeRow, beforeCol);
        }

        @Override
        public void redo(TextEditor editor) {
            for (LineChange change : changes) {
                editor.lines.set(change.row(), new StringBuilder(change.after()));
            }
            editor.setCursor(afterRow, afterCol);
        }
    }

    private void requireInsertMode() {
        if (mode != Mode.INSERT) {
            throw new IllegalStateException("Operation is not allowed in normal mode");
        }
    }

    private void validateTarget(String target) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target must not be null or empty");
        }
    }

    private int findNextWordStart(String text, int index) {
        int i = Math.min(index, text.length());

        if (i < text.length() && isWordChar(text.charAt(i))) {
            while (i < text.length() && isWordChar(text.charAt(i))) {
                i++;
            }
        }

        while (i < text.length() && !isWordChar(text.charAt(i))) {
            i++;
        }

        return i < text.length() ? i : -1;
    }

    private int findPreviousWordStart(String text, int index) {
        int i = Math.min(index - 1, text.length() - 1);

        while (i >= 0 && !isWordChar(text.charAt(i))) {
            i--;
        }

        if (i < 0) {
            return -1;
        }

        while (i > 0 && isWordChar(text.charAt(i - 1))) {
            i--;
        }

        return i;
    }

    private int findCurrentWordEnd(String text, int index) {
        if (index >= text.length() || !isWordChar(text.charAt(index))) {
            return -1;
        }

        int i = index;
        while (i < text.length() && isWordChar(text.charAt(i))) {
            i++;
        }
        return i;
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private int toTextIndex(int cursorRow, int cursorCol) {
        int index = cursorCol;
        for (int i = 0; i < cursorRow; i++) {
            index += lines.get(i).length() + 1;
        }
        return index;
    }

    private void moveCursorToTextIndex(int index) {
        int remaining = index;
        for (int i = 0; i < lines.size(); i++) {
            int lineLength = lines.get(i).length();
            if (remaining <= lineLength) {
                row = i;
                col = remaining;
                return;
            }
            remaining -= lineLength + 1;
        }

        row = lines.size() - 1;
        col = lines.get(row).length();
    }
}
