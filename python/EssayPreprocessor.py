import re

class EssayPreprocessor:
    def __init__(self):
        self.whitespace_re = re.compile(r'\s+')
        self.punctuation_re = re.compile(r'\s([.,;:!?])')
        self.question_words = {
            'discuss', 'extent', 'advantages', 'disadvantages',
            'opinion', 'view', 'agree', 'disagree'
        }

    def clean_essay(self, essay: str) -> str:
        if not essay:
            return ""
        cleaned = self.whitespace_re.sub(' ', essay)
        return self.punctuation_re.sub(r'\1', cleaned).strip()

    def count_words(self, text: str) -> int:
        return len(text.split()) if text else 0

    def extract_main_topic(self, question: str) -> str:
        if not question:
            return "general"
        
        lower_q = question.lower()
        for word in self.question_words:
            lower_q = lower_q.replace(word, '')
        
        sentences = re.split(r'[.,]', lower_q)
        first_sentence = sentences[0].strip() if sentences else ""
        return first_sentence or "general"