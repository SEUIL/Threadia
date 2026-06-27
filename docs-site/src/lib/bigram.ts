// 한글은 인접 2글자씩 겹치는 bigram으로 쪼개고, 그 외(영문/숫자/코드)는 단어 단위를 유지한다.
// Elasticsearch/Lucene의 CJK bigram 토큰 필터와 같은 방식이다.
const HANGUL = /[가-힣]/;
const WHITESPACE = /\s/;

export function toBigramText(text: string): string {
  const out: string[] = [];
  let hangulBuf = "";
  let otherBuf = "";

  const flushHangul = () => {
    if (!hangulBuf) return;
    if (hangulBuf.length === 1) {
      out.push(hangulBuf);
    } else {
      for (let i = 0; i < hangulBuf.length - 1; i++) {
        out.push(hangulBuf.slice(i, i + 2));
      }
    }
    hangulBuf = "";
  };

  const flushOther = () => {
    if (otherBuf) out.push(otherBuf);
    otherBuf = "";
  };

  for (const ch of text) {
    if (HANGUL.test(ch)) {
      flushOther();
      hangulBuf += ch;
    } else if (WHITESPACE.test(ch)) {
      flushHangul();
      flushOther();
    } else {
      flushHangul();
      otherBuf += ch;
    }
  }
  flushHangul();
  flushOther();
  return out.join(" ");
}
