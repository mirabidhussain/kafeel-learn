"""Copy the web app into the Android assets with everything bundled locally (no internet needed)."""
import re, pathlib, shutil, sys
root = pathlib.Path(__file__).resolve().parent.parent
fonts = pathlib.Path(sys.argv[1])          # folder holding baloo-2 latin woff2 files
out = root / "android/app/src/main/assets"
(out / "fonts").mkdir(parents=True, exist_ok=True)
html = (root / "index.html").read_text(encoding="utf-8")

faces = []
for w in (500, 700, 800):
    src = fonts / f"baloo-2-latin-{w}-normal.woff2"
    shutil.copy(src, out / "fonts" / src.name)
    faces.append(f"@font-face{{font-family:'Baloo 2';font-style:normal;font-weight:{w};"
                 f"font-display:swap;src:url('fonts/{src.name}') format('woff2')}}")

# drop every online-only reference
html = re.sub(r'\s*<link rel="preconnect"[^>]*>', "", html)
html = re.sub(r'\s*<link href="https://fonts\.googleapis\.com[^>]*>', "", html)
html = re.sub(r'\s*<link rel="manifest"[^>]*>', "", html)
html = html.replace("<style>", "<style>\n  " + "\n  ".join(faces), 1)
assert "googleapis" not in html and "http://" not in html.replace("http://www.w3.org", "")
(out / "index.html").write_text(html, encoding="utf-8")
shutil.copy(root / "icon-192.png", out / "icon-192.png")
print("offline assets ready:", sorted(p.name for p in out.rglob("*") if p.is_file()))
