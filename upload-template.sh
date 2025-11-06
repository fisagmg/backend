#!/bin/bash

# S3 템플릿 파일 업로드 스크립트
# 사용법: ./upload-template.sh /path/to/your/file.docx

if [ "$#" -ne 1 ]; then
    echo "사용법: ./upload-template.sh <docx파일경로>"
    echo "예시: ./upload-template.sh ~/Desktop/report.docx"
    exit 1
fi

FILE_PATH=$1
BUCKET_NAME=$(grep AWS_S3_BUCKET_NAME .env | cut -d '=' -f2)

if [ -z "$BUCKET_NAME" ]; then
    echo "❌ .env 파일에서 AWS_S3_BUCKET_NAME을 찾을 수 없습니다."
    exit 1
fi

echo "📤 업로드 중..."
echo "파일: $FILE_PATH"
echo "버킷: s3://$BUCKET_NAME/template/report_base.docx"

aws s3 cp "$FILE_PATH" "s3://$BUCKET_NAME/template/report_base.docx"

if [ $? -eq 0 ]; then
    echo "✅ 업로드 완료!"
    echo ""
    echo "확인:"
    aws s3 ls "s3://$BUCKET_NAME/template/"
else
    echo "❌ 업로드 실패"
fi

