@echo off

echo ===================================================================
echo                       广州律师协会自动学习客户端
echo                   学习目标网址：http://www.gzlawyer.org
echo                            联系QQ: 309259716
echo ===================================================================
echo.
set /p c=1.请输入cookie信息，需要自行登陆网址复制cookie信息：
set /p p=2.请输入学习第几页，多个用逗号分隔：
java -Dfile.encoding=utf-8 -jar gzlawyer.jar -p %p% -c "%c%"
pause