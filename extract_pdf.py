import pypdf
import sys

def extract_text_from_pdf(pdf_path):
    try:
        with open(pdf_path, 'rb') as file:
            reader = pypdf.PdfReader(file)
            text = ""
            for page in reader.pages:
                text += page.extract_text() + "\n"
            return text
    except Exception as e:
        print(f"Erro ao extrair texto do PDF: {e}")
        return None

if __name__ == "__main__":
    pdf_path = "1939397-Projeto_1.pdf"
    text = extract_text_from_pdf(pdf_path)
    if text:
        print("=== CONTEÚDO DO PDF ===")
        print(text)
    else:
        print("Não foi possível extrair o texto do PDF")
