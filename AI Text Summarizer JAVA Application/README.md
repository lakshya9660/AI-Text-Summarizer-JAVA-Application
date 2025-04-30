# AI Text Summarizer

A JavaFX desktop application that uses AI to generate summaries from various input sources including text, PDF files, and web URLs.

## Features

- Multiple input methods:
  - Direct text input
  - PDF file upload
  - Web URL content extraction
- AI-powered summarization using HuggingFace's facebook/bart-large-cnn model
- Multiple summary styles:
  - Standard Summary
  - Bullet Point Summary
  - Executive Summary
  - Technical Summary
- Different perspective views:
  - Executive
  - Student
  - Developer
  - Debate Mode
- Modern and intuitive user interface

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Maven
- HuggingFace API key

## Setup

1. Clone the repository:
```bash
git clone https://github.com/lakshya9660/AI-Text-Summarizer-JAVA-Application.git
cd ai-text-summarizer
```

2. Build the project:
```bash
mvn clean package
```

3. Run the application:
```bash
mvn javafx:run
```

## Usage

1. When you first launch the application, you'll be prompted to enter your HuggingFace API key.
2. Choose your input method:
   - Type or paste text directly into the input area
   - Upload a PDF file using the "Upload PDF" button
   - Enter a URL and click "Fetch" to extract content from a webpage
3. Select your desired summary style and perspective
4. Click "Summarize" to generate the summary
5. The summary will appear in the output area on the right

## Getting a HuggingFace API Key

1. Create an account at [HuggingFace](https://huggingface.co/)
2. Go to your profile settings
3. Navigate to the "Access Tokens" section
4. Create a new token with read access
5. Copy the token and use it when prompted by the application

## Dependencies

- JavaFX 17
- Apache PDFBox
- JSoup
- OkHttp
- Jackson

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
