const axios = require('axios');
const xml2js = require('xml2js');

exports.handler = async (event) => {
    try {
        // クエリパラメータからURLを取得
        const queryParams = event.queryStringParameters || {};
        const url = queryParams.url;

        if (!url) {
            return {
                statusCode: 400,
                body: JSON.stringify({ error: "URL parameter is required" })
            };
        }

        // RSSフィードを取得
        const response = await axios.get(url, { timeout: 10000 });
        const xmlData = response.data;

        // XMLをJSONに変換
        const parser = new xml2js.Parser({ explicitArray: false });
        const result = await parser.parseStringPromise(xmlData);

        const items = result.rss.channel.item.map(item => ({
            title: item.title,
            link: item.link,
            description: item.description,
            pubDate: item.pubDate
        }));

        return {
            statusCode: 200,
            body: JSON.stringify(items),
            headers: { "Content-Type": "application/json" }
        };
    } catch (error) {
        return {
            statusCode: 500,
            body: JSON.stringify({ error: "Failed to fetch RSS", details: error.message })
        };
    }
};
