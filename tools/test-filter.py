"""Runs the actual production matcher/interceptor against a local HTTP server. Requires JDK 17 and Python 3."""
from pathlib import Path
import os, subprocess, urllib.request
root=Path(__file__).resolve().parents[1]
out=root/'build'/'filter-tests';out.mkdir(parents=True,exist_ok=True)
artifacts={
 'okhttp.jar':'com/squareup/okhttp3/okhttp/3.12.13/okhttp-3.12.13.jar',
 'okio.jar':'com/squareup/okio/okio/1.15.0/okio-1.15.0.jar',
 'mockwebserver.jar':'com/squareup/okhttp3/mockwebserver/3.12.13/mockwebserver-3.12.13.jar',
 'junit.jar':'junit/junit/4.13.2/junit-4.13.2.jar',
 'hamcrest.jar':'org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar'}
for name,path in artifacts.items():
 if not (out/name).exists():urllib.request.urlretrieve('https://repo.maven.apache.org/maven2/'+path,out/name)
java=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else Path('')
cp=os.pathsep.join(str(out/n) for n in artifacts)
src=root/'common/src/main/java/com/liskovsoft/smartyoutubetv2/common/filter'
subprocess.run([str(java/'javac'),'--release','8','-cp',cp,'-d',str(out),str(src/'DomainFilter.java'),str(src/'FilterInterceptor.java'),str(root/'tools/tests/FilterTests.java')],check=True)
subprocess.run([str(java/'java'),'-cp',str(out)+os.pathsep+cp,'FilterTests'],check=True)
