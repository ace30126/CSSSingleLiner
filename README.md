# CSSSingleLiner

A tool to compress CSS rules onto single lines with syntax highlighting for improved readability.

## Overview

While working on personal web projects, I found that CSS files can quickly become lengthy and difficult to parse at a glance, especially when individual style rules span multiple lines. This increases scrolling and makes it harder to quickly find specific rules or understand the overall structure.
CSSSingleLiner was created to address this inconvenience! It takes a CSS file and neatly compresses each rule block (`selector { property: value; }`) onto a single line. Simultaneously, it applies syntax highlighting, differentiating selectors, properties, and values with distinct colors. This formatting is also applied to rules nested within `@media` queries (responsive queries), helping you manage even complex and long CSS files more effectively.

**Key Features:**

* Compresses multi-line CSS rules into single, concise lines.
* Applies syntax highlighting for better code comprehension.
* Correctly handles and formats rules within `@media` blocks.
* Real-time option to remove CSS comments (`/* ... */`).
* Automatically reduces excessive blank lines (3+ consecutive lines become 2) for cleaner output.
* Simple drag-and-drop interface for loading CSS files.
* Instant processing and display of formatted results.

## Installation

As this is a standalone executable (`CSSSingleLiner.exe`), no formal installation is required. Simply download the file and run it.

**Requirement:** This application requires **Java Runtime Environment (JRE) version 1.8.0 or higher** to be installed on your system.

## Usage

The application features a single-screen interface (Wow! Pseudo-SPA!).

1.  **Launch the Application:** Double-click `CSSSingleLiner.exe`.
2.  **Load CSS File:** Drag and drop the single CSS file you want to format onto the gray panel area located on the **left side** of the window.
3.  **View Result:** Processing occurs immediately upon dropping the file. The formatted CSS content will appear in the text area on the **right side**.
    * Each CSS rule will be presented on a single line.
    * Syntax elements (selectors, properties, values, etc.) will be colored differently based on standard CSS syntax.
    * A horizontal scrollbar will appear if the formatted content is wider than the text area.
4.  **Toggle Comments (Optional):** Locate the **"Remove Comments"** checkbox at the top of the left panel.
    * **Check** the box to remove all CSS comments (`/* comment content */`) from the output displayed on the right. The update happens in real-time.
    * **Uncheck** the box to include the comments back in the displayed output.
5.  **Automatic Blank Line Reduction:** Note that if your original CSS contains 3 or more consecutive blank lines, the output displayed on the right will automatically reduce them to just 2 blank lines for improved readability.

## Built With

* [JAVA](https://www.java.com/) - The programming language used (Requires JRE 1.8.0+).
* [Swing](https://docs.oracle.com/javase/8/docs/api/javax/swing/package-summary.html) - The GUI toolkit for Java.

## Author

* REDUCTO (https://tutoreducto.tistory.com/673)

## Creation Date

* May 6, 2025 (1 day of development)

## Version

* v1.0

## Notes

* **Distribution:** Unauthorized distribution of this program is prohibited. Please leave a comment if you are interested in using or sharing it.
* **Customization:** If you need custom features or modifications, please leave a comment to discuss.

## Acknowledgements

* This program was created with the assistance of Gemini.
