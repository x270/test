from lxml import etree
import json
import requests
import xml.etree.ElementTree as ET

def lambda_handler(event, context):
    try:
        # クエリパラメータからURLを取得
        query_params = event.get("queryStringParameters", {})
        url = query_params.get("url")

        if not url:
            return {
                "statusCode": 400,
                "body": json.dumps({"error": "URL parameter is required"})
            }

        # RSSフィードを取得
        response = requests.get(url, timeout=10)
        response.raise_for_status()

        # XMLをパース
        parser = etree.XMLParser()
        document = etree.fromstring(response.text, parser)
        root = ET.fromstring(response.text)
        items = []

        for item in root.findall(".//item"):
            parsed_item = {
                "title": item.findtext("title"),
                "link": item.findtext("link"),
                "description": item.findtext("description"),
                "pubDate": item.findtext("pubDate")
            }
            items.append(parsed_item)

        return {
            "statusCode": 200,
            "body": json.dumps(items),
            "headers": {"Content-Type": "application/json"}
        }

    except requests.exceptions.RequestException as e:
        return {
            "statusCode": 500,
            "body": json.dumps({"error": "Failed to fetch RSS", "details": str(e)})
        }
    except ET.ParseError:
        return {
            "statusCode": 500,
            "body": json.dumps({"error": "Invalid RSS format"})
        }
