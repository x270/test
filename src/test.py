import requests
from lxml import etree
import json

def lambda_handler(event, context):
    # URLをクエリパラメータから取得
    url = event['queryStringParameters']['url']

    try:
        # URLへGETリクエスト
        response = requests.get(url)
        response.raise_for_status()  # HTTPエラーをチェック

        # XMLをパース
        root = etree.fromstring(response.content)

        # item要素を抽出
        items = []
        for item in root.findall('.//item'):
            item_data = {}
            for child in item:
                item_data[child.tag] = child.text
            items.append(item_data)

        # JSONに変換
        json_data = json.dumps(items, ensure_ascii=False)

        return {
            'statusCode': 200,
            'headers': {
                'Content-Type': 'application/json; charset=utf-8'
            },
            'body': json_data
        }

    except requests.exceptions.RequestException as e:
        return {
            'statusCode': 500,
            'body': f'Error: {e}'
        }
    except etree.XMLSyntaxError as e:
        return {
            'statusCode': 500,
            'body': f'Error: Invalid XML format: {e}'
        }
    except Exception as e:
        return {
            'statusCode': 500,
            'body': f'Error: {e}'
        }
